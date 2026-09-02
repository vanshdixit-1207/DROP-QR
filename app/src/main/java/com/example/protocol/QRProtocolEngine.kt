package com.example.protocol

import android.util.Base64
import com.example.crypto.CryptoEngine
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID

class PasswordRequiredException(val hint: String = "") : Exception("This transfer is protected with a custom Password / PIN")
class IncorrectPasswordException(message: String = "Incorrect decryption password / PIN. Please try again.") : Exception(message)

object QRProtocolEngine {

    const val PROTOCOL_VERSION = "DQR1"
    private const val DELIMITER = "|"
    const val DEFAULT_CHUNK_SIZE_BYTES = 320

    /**
     * Serializes a QRFrame into the wire string format:
     * DQR1|transferId|TYPE|index|total|crc32Hex|base64Payload
     */
    fun encodeFrame(frame: QRFrame): String {
        return "${frame.version}$DELIMITER${frame.transferId}$DELIMITER${frame.type.code}$DELIMITER${frame.frameIndex}$DELIMITER${frame.totalFrames}$DELIMITER${frame.crc32Hex}$DELIMITER${frame.payloadChunk}"
    }

    /**
     * Parses a raw QR code string into a QRFrame.
     * Returns null if the string is not a valid DropQR frame or has an invalid CRC32.
     */
    fun decodeFrame(rawQrText: String): QRFrame? {
        if (!rawQrText.startsWith(PROTOCOL_VERSION)) return null

        val parts = rawQrText.split(DELIMITER, limit = 7)
        if (parts.size < 7) return null

        val version = parts[0]
        val transferId = parts[1]
        val typeCode = parts[2]
        val frameIndex = parts[3].toIntOrNull() ?: return null
        val totalFrames = parts[4].toIntOrNull() ?: return null
        val expectedCrc32 = parts[5]
        val payloadChunk = parts[6]

        if (frameIndex <= 0 || totalFrames <= 0 || frameIndex > totalFrames) return null

        // Validate payload chunk CRC32
        try {
            val chunkBytes = Base64.decode(payloadChunk, Base64.NO_WRAP)
            val actualCrc32 = CryptoEngine.computeCrc32(chunkBytes)
            if (!actualCrc32.equals(expectedCrc32, ignoreCase = true)) {
                return null
            }

            return QRFrame(
                version = version,
                transferId = transferId,
                type = TransferPayloadType.fromCode(typeCode),
                frameIndex = frameIndex,
                totalFrames = totalFrames,
                crc32Hex = expectedCrc32,
                payloadChunk = payloadChunk
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Prepares full binary package, compresses, encrypts, and chunks it into QRFrames.
     */
    fun createTransferFrames(
        type: TransferPayloadType,
        title: String,
        payloadBytes: ByteArray,
        files: List<TransferFileItem> = emptyList(),
        chunkSizeBytes: Int = DEFAULT_CHUNK_SIZE_BYTES,
        encrypt: Boolean = true,
        compress: Boolean = true,
        customPassword: String? = null,
        passwordHint: String? = null,
        timeLockUntil: Long = 0L
    ): Pair<TransferManifest, List<QRFrame>> {
        val transferId = UUID.randomUUID().toString().take(8).uppercase()
        val overallSha256 = CryptoEngine.computeSha256(payloadBytes)
        val hasCustomPassword = !customPassword.isNullOrBlank()

        // 1. Build Header JSON
        val headerJson = JSONObject().apply {
            put("id", transferId)
            put("type", type.code)
            put("title", title)
            put("size", payloadBytes.size.toLong())
            put("sha256", overallSha256)
            put("time", System.currentTimeMillis())
            put("pwd", hasCustomPassword)
            if (timeLockUntil > 0L) {
                put("timeLock", timeLockUntil)
            }
            if (hasCustomPassword && !passwordHint.isNullOrBlank()) {
                put("pwdHint", passwordHint)
            }
            val filesArray = JSONArray()
            files.forEach { file ->
                val fObj = JSONObject().apply {
                    put("name", file.fileName)
                    put("size", file.fileSize)
                    put("mime", file.mimeType)
                    put("sha256", file.checksumSha256)
                }
                filesArray.put(fObj)
            }
            put("files", filesArray)
        }

        val headerBytes = headerJson.toString().toByteArray(Charsets.UTF_8)
        val packageBuffer = ByteBuffer.allocate(4 + headerBytes.size + payloadBytes.size)
        packageBuffer.putInt(headerBytes.size)
        packageBuffer.put(headerBytes)
        packageBuffer.put(payloadBytes)
        var packagedBytes = packageBuffer.array()

        // 2. Compression
        var isCompressed = false
        if (compress) {
            val compressed = CryptoEngine.compressGzip(packagedBytes)
            if (compressed.size < packagedBytes.size) {
                packagedBytes = compressed
                isCompressed = true
            }
        }

        // 3. Encryption
        var isEncrypted = false
        if (encrypt || hasCustomPassword) {
            val sessionKey = if (hasCustomPassword) {
                CryptoEngine.derivePasswordKey(customPassword!!, "DropQR-AirGap-$transferId")
            } else {
                CryptoEngine.deriveKey(transferId)
            }
            packagedBytes = CryptoEngine.encryptAesGcm(packagedBytes, sessionKey)
            isEncrypted = true
        }

        // 4. Chunking
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < packagedBytes.size) {
            val length = minOf(chunkSizeBytes, packagedBytes.size - offset)
            val chunk = ByteArray(length)
            System.arraycopy(packagedBytes, offset, chunk, 0, length)
            chunks.add(chunk)
            offset += length
        }

        if (chunks.isEmpty()) {
            chunks.add(ByteArray(0))
        }

        val totalFrames = chunks.size
        val qrFrames = chunks.mapIndexed { index, chunk ->
            val crc32 = CryptoEngine.computeCrc32(chunk)
            val base64Payload = Base64.encodeToString(chunk, Base64.NO_WRAP)
            QRFrame(
                version = PROTOCOL_VERSION,
                transferId = transferId,
                type = type,
                frameIndex = index + 1,
                totalFrames = totalFrames,
                crc32Hex = crc32,
                payloadChunk = base64Payload
            )
        }

        val manifest = TransferManifest(
            transferId = transferId,
            type = type,
            title = title,
            totalBytes = payloadBytes.size.toLong(),
            totalFrames = totalFrames,
            isEncrypted = isEncrypted,
            isCompressed = isCompressed,
            overallSha256 = overallSha256,
            files = files,
            isPasswordProtected = hasCustomPassword,
            passwordHint = passwordHint ?: "",
            timeLockUntil = timeLockUntil
        )

        return Pair(manifest, qrFrames)
    }

    /**
     * Reassembles a full set of chunk frames, decrypts, decompresses, and unpacks the payload.
     */
    fun unpackTransfer(
        transferId: String,
        receivedChunksMap: Map<Int, String>, // 1-based index -> Base64 payload
        totalFrames: Int,
        isEncrypted: Boolean = true,
        customPassword: String? = null
    ): UnpackedPayload {
        require(receivedChunksMap.size == totalFrames) { "Cannot unpack incomplete transfer (${receivedChunksMap.size} / $totalFrames frames)" }

        // 1. Reassemble chunks in exact order
        val allChunkBytes = mutableListOf<ByteArray>()
        for (i in 1..totalFrames) {
            val b64 = receivedChunksMap[i] ?: throw IllegalStateException("Missing chunk #$i")
            allChunkBytes.add(Base64.decode(b64, Base64.NO_WRAP))
        }

        val totalSize = allChunkBytes.sumOf { it.size }
        val combinedBytes = ByteArray(totalSize)
        var offset = 0
        for (chunk in allChunkBytes) {
            System.arraycopy(chunk, 0, combinedBytes, offset, chunk.size)
            offset += chunk.size
        }

        var currentBytes = combinedBytes

        // 2. Decrypt if needed
        if (isEncrypted) {
            if (!customPassword.isNullOrBlank()) {
                try {
                    val passKey = CryptoEngine.derivePasswordKey(customPassword, "DropQR-AirGap-$transferId")
                    currentBytes = CryptoEngine.decryptAesGcm(currentBytes, passKey)
                } catch (e: Exception) {
                    throw IncorrectPasswordException()
                }
            } else {
                try {
                    // Try default transfer key first
                    val sessionKey = CryptoEngine.deriveKey(transferId)
                    currentBytes = CryptoEngine.decryptAesGcm(currentBytes, sessionKey)
                } catch (e: Exception) {
                    // If default key failed, it is likely password protected
                    throw PasswordRequiredException()
                }
            }
        }

        // 3. Try GZIP decompress (auto-detect GZIP magic header 0x1F, 0x8B)
        if (currentBytes.size >= 2 && currentBytes[0] == 0x1F.toByte() && currentBytes[1] == 0x8B.toByte()) {
            try {
                currentBytes = CryptoEngine.decompressGzip(currentBytes)
            } catch (_: Exception) {
                // Not gzip compressed or raw data
            }
        }

        // 4. Unpack Package Buffer [4 bytes header length] + [Header JSON] + [Payload]
        if (currentBytes.size < 4) {
            throw IllegalStateException("Decrypted payload data is corrupted or incomplete")
        }

        val buffer = ByteBuffer.wrap(currentBytes)
        val headerLength = buffer.int
        if (headerLength <= 0 || headerLength > buffer.remaining()) {
            throw IllegalStateException("Corrupted package header length")
        }

        val headerBytes = ByteArray(headerLength)
        buffer.get(headerBytes)

        val headerJson = JSONObject(String(headerBytes, Charsets.UTF_8))
        val typeCode = headerJson.optString("type", "FIL")
        val title = headerJson.optString("title", "Transferred Item")
        val originalSha256 = headerJson.optString("sha256", "")
        val isPwd = headerJson.optBoolean("pwd", false)
        val pwdHint = headerJson.optString("pwdHint", "")
        val timeLockUntil = headerJson.optLong("timeLock", 0L)

        val payloadBytes = ByteArray(buffer.remaining())
        buffer.get(payloadBytes)

        // Verify reassembled SHA-256
        val calculatedSha256 = CryptoEngine.computeSha256(payloadBytes)
        val sha256Valid = originalSha256.isEmpty() || originalSha256.equals(calculatedSha256, ignoreCase = true)

        val filesList = mutableListOf<TransferFileItem>()
        val filesArray = headerJson.optJSONArray("files")
        if (filesArray != null) {
            for (i in 0 until filesArray.length()) {
                val fObj = filesArray.getJSONObject(i)
                filesList.add(
                    TransferFileItem(
                        fileName = fObj.optString("name", "file_$i"),
                        fileSize = fObj.optLong("size", 0L),
                        mimeType = fObj.optString("mime", "application/octet-stream"),
                        checksumSha256 = fObj.optString("sha256", "")
                    )
                )
            }
        }

        return UnpackedPayload(
            transferId = transferId,
            type = TransferPayloadType.fromCode(typeCode),
            title = title,
            payloadBytes = payloadBytes,
            files = filesList,
            sha256Verified = sha256Valid,
            computedSha256 = calculatedSha256,
            isPasswordProtected = isPwd,
            passwordHint = pwdHint,
            timeLockUntil = timeLockUntil
        )
    }
}

data class UnpackedPayload(
    val transferId: String,
    val type: TransferPayloadType,
    val title: String,
    val payloadBytes: ByteArray,
    val files: List<TransferFileItem>,
    val sha256Verified: Boolean,
    val computedSha256: String,
    val isPasswordProtected: Boolean = false,
    val passwordHint: String = "",
    val timeLockUntil: Long = 0L
)
