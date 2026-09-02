package com.example.stego

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.crypto.CryptoEngine
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

data class StegoSecretMetadata(
    val title: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val timeLockUntil: Long = 0L,
    val isPasswordProtected: Boolean = false,
    val passwordHint: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sha256: String = ""
)

data class ExtractedSecret(
    val metadata: StegoSecretMetadata,
    val payloadBytes: ByteArray,
    val textContent: String? = null,
    val savedFilePath: String? = null,
    val isLockedByTime: Boolean = false,
    val timeRemainingMillis: Long = 0L
)

object SteganographyEngine {

    private val MAGIC_BYTES = "DQR_STEG_V1".toByteArray(Charsets.UTF_8)
    private const val BITS_PER_CHANNEL = 2 // 2 LSB bits per R, G, B channel for optimum balance of capacity & stealth

    /**
     * Calculates the maximum payload bytes that can be hidden inside a Bitmap of given width and height.
     */
    fun calculateCapacityBytes(width: Int, height: Int): Int {
        val totalChannels = width * height * 3 // R, G, B
        val totalBits = totalChannels * BITS_PER_CHANNEL
        val totalBytes = totalBits / 8
        // Reserve 256 bytes for headers and safety
        return maxOf(0, totalBytes - 256)
    }

    /**
     * Embeds secret payload and metadata invisibly inside a cover Bitmap.
     * Returns a new Bitmap containing the hidden data.
     */
    fun hideSecretInBitmap(
        coverBitmap: Bitmap,
        title: String,
        fileName: String,
        mimeType: String,
        secretBytes: ByteArray,
        customPassword: String? = null,
        passwordHint: String? = null,
        timeLockUntil: Long = 0L
    ): Bitmap {
        val hasPassword = !customPassword.isNullOrBlank()
        val overallSha256 = CryptoEngine.computeSha256(secretBytes)

        // 1. Prepare Header Metadata
        val metaJson = JSONObject().apply {
            put("title", title)
            put("name", fileName)
            put("mime", mimeType)
            put("size", secretBytes.size.toLong())
            put("time", System.currentTimeMillis())
            put("timeLock", timeLockUntil)
            put("sha256", overallSha256)
            put("pwd", hasPassword)
            if (hasPassword && !passwordHint.isNullOrBlank()) {
                put("hint", passwordHint)
            }
        }

        val metaBytes = metaJson.toString().toByteArray(Charsets.UTF_8)

        // 2. Compress & Encrypt Secret Payload
        var processedPayload = CryptoEngine.compressGzip(secretBytes)
        if (hasPassword) {
            val key = CryptoEngine.derivePasswordKey(customPassword!!, "DropQR-Stego-Salt")
            processedPayload = CryptoEngine.encryptAesGcm(processedPayload, key)
        } else {
            val key = CryptoEngine.deriveKey("DropQR-Stego-Default")
            processedPayload = CryptoEngine.encryptAesGcm(processedPayload, key)
        }

        // 3. Assemble binary envelope:
        // [MAGIC 11 bytes] + [4 bytes Meta Len] + [Meta Bytes] + [4 bytes Payload Len] + [Payload Bytes]
        val totalEnvSize = MAGIC_BYTES.size + 4 + metaBytes.size + 4 + processedPayload.size
        val envelope = ByteBuffer.allocate(totalEnvSize)
        envelope.put(MAGIC_BYTES)
        envelope.putInt(metaBytes.size)
        envelope.put(metaBytes)
        envelope.putInt(processedPayload.size)
        envelope.put(processedPayload)
        val envelopeBytes = envelope.array()

        val capacity = calculateCapacityBytes(coverBitmap.width, coverBitmap.height)
        if (envelopeBytes.size > capacity) {
            throw IllegalArgumentException(
                "Cover image is too small! Need ${envelopeBytes.size / 1024} KB capacity, but image only holds ${capacity / 1024} KB. Please use a larger image."
            )
        }

        // 4. Encode into Bitmap pixels
        val width = coverBitmap.width
        val height = coverBitmap.height
        val outputBitmap = coverBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        outputBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var byteIndex = 0
        var bitOffset = 0 // 0 to 7
        val totalEnvelopeBytes = envelopeBytes.size

        val mask = (1 shl BITS_PER_CHANNEL) - 1 // 0b11 for 2 bits
        val clearMask = mask.inv() and 0xFF

        for (i in pixels.indices) {
            if (byteIndex >= totalEnvelopeBytes) break

            var pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF

            // Encode into Red channel
            if (byteIndex < totalEnvelopeBytes) {
                val bits = (envelopeBytes[byteIndex].toInt() shr bitOffset) and mask
                r = (r and clearMask) or bits
                bitOffset += BITS_PER_CHANNEL
                if (bitOffset >= 8) {
                    bitOffset = 0
                    byteIndex++
                }
            }

            // Encode into Green channel
            if (byteIndex < totalEnvelopeBytes) {
                val bits = (envelopeBytes[byteIndex].toInt() shr bitOffset) and mask
                g = (g and clearMask) or bits
                bitOffset += BITS_PER_CHANNEL
                if (bitOffset >= 8) {
                    bitOffset = 0
                    byteIndex++
                }
            }

            // Encode into Blue channel
            if (byteIndex < totalEnvelopeBytes) {
                val bits = (envelopeBytes[byteIndex].toInt() shr bitOffset) and mask
                b = (b and clearMask) or bits
                bitOffset += BITS_PER_CHANNEL
                if (bitOffset >= 8) {
                    bitOffset = 0
                    byteIndex++
                }
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }

    /**
     * Checks if a Bitmap contains DropQR hidden steganography data.
     */
    fun hasHiddenData(bitmap: Bitmap): Boolean {
        return try {
            val magic = readBytesFromBitmap(bitmap, 0, MAGIC_BYTES.size)
            magic.contentEquals(MAGIC_BYTES)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts and decrypts hidden secret from a steganography Bitmap.
     */
    fun extractSecretFromBitmap(
        context: Context,
        bitmap: Bitmap,
        customPassword: String? = null
    ): ExtractedSecret {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val mask = (1 shl BITS_PER_CHANNEL) - 1

        fun extractRawBytes(startByteOffset: Int, count: Int): ByteArray {
            val result = ByteArray(count)
            var currentByte = 0
            var bitOffset = 0
            var extractedBytesCount = 0

            // Skip bits until startByteOffset
            var bitsToSkip = startByteOffset * 8
            var currentBitPosition = 0

            for (pixel in pixels) {
                if (extractedBytesCount >= count) break

                val channels = intArrayOf(
                    (pixel shr 16) and 0xFF,
                    (pixel shr 8) and 0xFF,
                    pixel and 0xFF
                )

                for (ch in channels) {
                    if (currentBitPosition < bitsToSkip) {
                        currentBitPosition += BITS_PER_CHANNEL
                        continue
                    }
                    if (extractedBytesCount >= count) break

                    val bits = ch and mask
                    currentByte = currentByte or (bits shl bitOffset)
                    bitOffset += BITS_PER_CHANNEL

                    if (bitOffset >= 8) {
                        result[extractedBytesCount] = currentByte.toByte()
                        extractedBytesCount++
                        currentByte = 0
                        bitOffset = 0
                    }
                }
            }

            return result
        }

        // 1. Verify Magic Bytes
        val magic = extractRawBytes(0, MAGIC_BYTES.size)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw IllegalArgumentException("No hidden DropQR secret found in this image.")
        }

        // 2. Read Metadata Length & Metadata
        var offset = MAGIC_BYTES.size
        val metaLenBytes = extractRawBytes(offset, 4)
        offset += 4
        val metaLen = ByteBuffer.wrap(metaLenBytes).int
        if (metaLen <= 0 || metaLen > 50000) {
            throw IllegalStateException("Corrupted steganography header length")
        }

        val metaBytes = extractRawBytes(offset, metaLen)
        offset += metaLen

        val metaJson = JSONObject(String(metaBytes, Charsets.UTF_8))
        val title = metaJson.optString("title", "Hidden Secret")
        val fileName = metaJson.optString("name", "secret_file")
        val mimeType = metaJson.optString("mime", "text/plain")
        val fileSize = metaJson.optLong("size", 0L)
        val timeLockUntil = metaJson.optLong("timeLock", 0L)
        val isPwd = metaJson.optBoolean("pwd", false)
        val pwdHint = metaJson.optString("hint", "")
        val sha256 = metaJson.optString("sha256", "")
        val timestamp = metaJson.optLong("time", System.currentTimeMillis())

        val metadata = StegoSecretMetadata(
            title = title,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            timeLockUntil = timeLockUntil,
            isPasswordProtected = isPwd,
            passwordHint = pwdHint,
            timestamp = timestamp,
            sha256 = sha256
        )

        // Check Time-Lock
        val now = System.currentTimeMillis()
        val isLocked = timeLockUntil > now
        val timeRemaining = if (isLocked) timeLockUntil - now else 0L

        // 3. Read Encrypted Payload
        val payloadLenBytes = extractRawBytes(offset, 4)
        offset += 4
        val payloadLen = ByteBuffer.wrap(payloadLenBytes).int
        if (payloadLen <= 0 || payloadLen > 50_000_000) {
            throw IllegalStateException("Invalid secret payload size")
        }

        val encryptedPayload = extractRawBytes(offset, payloadLen)

        // 4. Decrypt Payload
        var decryptedBytes: ByteArray
        if (isPwd) {
            if (customPassword.isNullOrBlank()) {
                throw com.example.protocol.PasswordRequiredException(pwdHint)
            }
            try {
                val key = CryptoEngine.derivePasswordKey(customPassword, "DropQR-Stego-Salt")
                decryptedBytes = CryptoEngine.decryptAesGcm(encryptedPayload, key)
            } catch (_: Exception) {
                throw com.example.protocol.IncorrectPasswordException()
            }
        } else {
            try {
                val key = CryptoEngine.deriveKey("DropQR-Stego-Default")
                decryptedBytes = CryptoEngine.decryptAesGcm(encryptedPayload, key)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to decrypt hidden data: ${e.localizedMessage}")
            }
        }

        // Decompress GZIP
        try {
            decryptedBytes = CryptoEngine.decompressGzip(decryptedBytes)
        } catch (_: Exception) {
            // Uncompressed fallback
        }

        var textContent: String? = null
        var savedFilePath: String? = null

        if (!isLocked) {
            if (mimeType.startsWith("text/") || mimeType == "application/json" || fileName.endsWith(".txt")) {
                textContent = String(decryptedBytes, Charsets.UTF_8)
            } else {
                // Save to app files
                val stegoDir = File(context.filesDir, "stego_extracted").apply { mkdirs() }
                val targetFile = File(stegoDir, "${System.currentTimeMillis()}_$fileName")
                FileOutputStream(targetFile).use { it.write(decryptedBytes) }
                savedFilePath = targetFile.absolutePath
            }
        }

        return ExtractedSecret(
            metadata = metadata,
            payloadBytes = decryptedBytes,
            textContent = textContent,
            savedFilePath = savedFilePath,
            isLockedByTime = isLocked,
            timeRemainingMillis = timeRemaining
        )
    }

    private fun readBytesFromBitmap(bitmap: Bitmap, startByteOffset: Int, count: Int): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val mask = (1 shl BITS_PER_CHANNEL) - 1
        val result = ByteArray(count)
        var currentByte = 0
        var bitOffset = 0
        var extractedBytesCount = 0

        var bitsToSkip = startByteOffset * 8
        var currentBitPosition = 0

        for (pixel in pixels) {
            if (extractedBytesCount >= count) break
            val channels = intArrayOf(
                (pixel shr 16) and 0xFF,
                (pixel shr 8) and 0xFF,
                pixel and 0xFF
            )
            for (ch in channels) {
                if (currentBitPosition < bitsToSkip) {
                    currentBitPosition += BITS_PER_CHANNEL
                    continue
                }
                if (extractedBytesCount >= count) break
                val bits = ch and mask
                currentByte = currentByte or (bits shl bitOffset)
                bitOffset += BITS_PER_CHANNEL
                if (bitOffset >= 8) {
                    result[extractedBytesCount] = currentByte.toByte()
                    extractedBytesCount++
                    currentByte = 0
                    bitOffset = 0
                }
            }
        }
        return result
    }

    /**
     * Saves a Stego Bitmap into a lossless PNG file.
     */
    fun saveStegoBitmapToPng(context: Context, bitmap: Bitmap, fileName: String = "Stego_Cover_${System.currentTimeMillis()}.png"): File {
        val outputDir = File(context.cacheDir, "stego_output").apply { mkdirs() }
        val file = File(outputDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
