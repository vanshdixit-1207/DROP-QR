package com.example.transfer

import android.content.Context
import com.example.data.TransferDirection
import com.example.data.TransferEntity
import com.example.data.TransferRepository
import com.example.data.TransferStatus
import com.example.protocol.ContactPayload
import com.example.protocol.QRFrame
import com.example.protocol.QRProtocolEngine
import com.example.protocol.TransferFileItem
import com.example.protocol.TransferPayloadType
import com.example.protocol.UnpackedPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

enum class ReceiverStateStatus {
    IDLE,
    CONNECTING,
    RECEIVING,
    VERIFYING,
    DECRYPTING,
    SAVING,
    SUCCESS,
    ERROR
}

data class SavedFileItem(
    val name: String,
    val size: Long,
    val mimeType: String,
    val localPath: String
)

data class ReceivedTransferResult(
    val transferId: String,
    val type: TransferPayloadType,
    val title: String,
    val textContent: String? = null,
    val contactData: ContactPayload? = null,
    val savedFiles: List<SavedFileItem> = emptyList(),
    val totalBytes: Long = 0L,
    val totalFrames: Int = 1,
    val sha256: String = "",
    val isEncrypted: Boolean = true,
    val transferDurationMs: Long = 0L,
    val savedHistoryId: Long = 0L
)

data class ReceiverUiState(
    val status: ReceiverStateStatus = ReceiverStateStatus.IDLE,
    val transferId: String = "",
    val type: TransferPayloadType = TransferPayloadType.TEXT,
    val title: String = "",
    val receivedFramesCount: Int = 0,
    val totalFrames: Int = 0,
    val missingFrames: List<Int> = emptyList(),
    val progressPercent: Int = 0,
    val speedFps: Float = 0f,
    val duplicateFramesCount: Int = 0,
    val result: ReceivedTransferResult? = null,
    val errorMessage: String = "",
    val canResume: Boolean = false
)

class ReceiverEngine(
    private val context: Context,
    private val transferRepository: TransferRepository,
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    private val chunksMap = ConcurrentHashMap<Int, String>()
    private var activeTransferId: String? = null
    private var activeTotalFrames = 0
    private var activeType = TransferPayloadType.FILE
    private var startTimeMs: Long = 0L
    private var frameReceiveTimestamps = mutableListOf<Long>()
    private var duplicateCount = 0

    fun onFrameScanned(rawQrText: String): Boolean {
        val frame = QRProtocolEngine.decodeFrame(rawQrText) ?: return false

        scope.launch(Dispatchers.Default) {
            handleFrame(frame)
        }
        return true
    }

    private suspend fun handleFrame(frame: QRFrame) {
        // If this is a new transfer or first frame
        if (activeTransferId == null || activeTransferId != frame.transferId) {
            if (activeTransferId != null && chunksMap.size > 0 && chunksMap.size < activeTotalFrames) {
                // Different transfer detected while in-progress
                // Keep existing transfer state unless user decides to switch or new frame has high index
            }
            activeTransferId = frame.transferId
            activeTotalFrames = frame.totalFrames
            activeType = frame.type
            startTimeMs = System.currentTimeMillis()
            chunksMap.clear()
            frameReceiveTimestamps.clear()
            duplicateCount = 0

            withContext(Dispatchers.Main) {
                _uiState.value = ReceiverUiState(
                    status = ReceiverStateStatus.RECEIVING,
                    transferId = frame.transferId,
                    type = frame.type,
                    title = "Transfer in progress...",
                    receivedFramesCount = 0,
                    totalFrames = frame.totalFrames,
                    missingFrames = (1..frame.totalFrames).toList(),
                    progressPercent = 0,
                    speedFps = 0f
                )
            }
        }

        // Check duplicate
        if (chunksMap.containsKey(frame.frameIndex)) {
            duplicateCount++
            return
        }

        // Store chunk
        chunksMap[frame.frameIndex] = frame.payloadChunk
        val now = System.currentTimeMillis()
        frameReceiveTimestamps.add(now)

        // Calculate FPS over last 2 seconds
        val twoSecsAgo = now - 2000
        val recentCount = frameReceiveTimestamps.count { it > twoSecsAgo }
        val fps = (recentCount / 2f).coerceAtLeast(0f)

        val receivedCount = chunksMap.size
        val total = activeTotalFrames
        val percent = ((receivedCount.toFloat() / total.coerceAtLeast(1)) * 100).toInt()
        val missing = (1..total).filter { !chunksMap.containsKey(it) }

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                status = ReceiverStateStatus.RECEIVING,
                receivedFramesCount = receivedCount,
                totalFrames = total,
                missingFrames = missing,
                progressPercent = percent,
                speedFps = fps,
                duplicateFramesCount = duplicateCount
            )
        }

        // If all frames collected, trigger reassembly
        if (receivedCount == total && total > 0) {
            finishReassembly()
        }
    }

    private suspend fun finishReassembly() {
        val transferId = activeTransferId ?: return
        val totalFrames = activeTotalFrames

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(status = ReceiverStateStatus.VERIFYING)
        }

        try {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(status = ReceiverStateStatus.DECRYPTING)
            }

            val unpacked: UnpackedPayload = withContext(Dispatchers.Default) {
                QRProtocolEngine.unpackTransfer(
                    transferId = transferId,
                    receivedChunksMap = HashMap(chunksMap),
                    totalFrames = totalFrames,
                    isEncrypted = true
                )
            }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(status = ReceiverStateStatus.SAVING)
            }

            // Save payload to disk / format output
            val durationMs = System.currentTimeMillis() - startTimeMs
            val result = saveUnpackedPayload(unpacked, totalFrames, durationMs)

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    status = ReceiverStateStatus.SUCCESS,
                    result = result,
                    progressPercent = 100
                )
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    status = ReceiverStateStatus.ERROR,
                    errorMessage = e.message ?: "Failed to reconstruct transferred data"
                )
            }
        }
    }

    private suspend fun saveUnpackedPayload(
        unpacked: UnpackedPayload,
        totalFrames: Int,
        durationMs: Long
    ): ReceivedTransferResult = withContext(Dispatchers.IO) {
        val transferDir = File(context.filesDir, "transfers").apply { mkdirs() }
        var textContent: String? = null
        var contactPayload: ContactPayload? = null
        val savedFiles = mutableListOf<SavedFileItem>()
        var primaryFilePath: String? = null

        when (unpacked.type) {
            TransferPayloadType.TEXT, TransferPayloadType.URL -> {
                val text = String(unpacked.payloadBytes, Charsets.UTF_8)
                textContent = text
            }
            TransferPayloadType.CONTACT -> {
                val vcard = String(unpacked.payloadBytes, Charsets.UTF_8)
                textContent = vcard
                contactPayload = ContactPayload.fromVCard(vcard)
                // Also save vcf file
                val safeName = "${unpacked.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.vcf"
                val file = File(transferDir, "${unpacked.transferId}_$safeName")
                FileOutputStream(file).use { it.write(unpacked.payloadBytes) }
                savedFiles.add(SavedFileItem(safeName, file.length(), "text/x-vcard", file.absolutePath))
                primaryFilePath = file.absolutePath
            }
            TransferPayloadType.FILE -> {
                val originalFileName = unpacked.files.firstOrNull()?.fileName ?: unpacked.title
                val safeName = originalFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val targetFile = File(transferDir, "${unpacked.transferId}_$safeName")
                FileOutputStream(targetFile).use { it.write(unpacked.payloadBytes) }
                val mime = unpacked.files.firstOrNull()?.mimeType ?: "application/octet-stream"
                savedFiles.add(SavedFileItem(safeName, targetFile.length(), mime, targetFile.absolutePath))
                primaryFilePath = targetFile.absolutePath
            }
            TransferPayloadType.MULTI_FILE -> {
                // Unpack sequential files from payload bytes
                val buffer = java.nio.ByteBuffer.wrap(unpacked.payloadBytes)
                unpacked.files.forEach { fileMeta ->
                    val fileLength = fileMeta.fileSize.toInt()
                    if (buffer.remaining() >= fileLength) {
                        val fileBytes = ByteArray(fileLength)
                        buffer.get(fileBytes)
                        val safeName = fileMeta.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        val targetFile = File(transferDir, "${unpacked.transferId}_$safeName")
                        FileOutputStream(targetFile).use { it.write(fileBytes) }
                        savedFiles.add(SavedFileItem(safeName, targetFile.length(), fileMeta.mimeType, targetFile.absolutePath))
                    }
                }
                primaryFilePath = savedFiles.firstOrNull()?.localPath
            }
        }

        // Save history entry to Room database
        val entity = TransferEntity(
            transferId = unpacked.transferId,
            direction = TransferDirection.RECEIVED,
            payloadType = unpacked.type.code,
            title = unpacked.title,
            subtitle = when (unpacked.type) {
                TransferPayloadType.TEXT -> textContent?.take(60) ?: ""
                TransferPayloadType.URL -> textContent ?: ""
                TransferPayloadType.CONTACT -> contactPayload?.name ?: ""
                TransferPayloadType.FILE -> "${savedFiles.size} file (${unpacked.payloadBytes.size} bytes)"
                TransferPayloadType.MULTI_FILE -> "${savedFiles.size} files (${unpacked.payloadBytes.size} bytes)"
            },
            sizeBytes = unpacked.payloadBytes.size.toLong(),
            frameCount = totalFrames,
            status = TransferStatus.COMPLETED,
            filePath = primaryFilePath,
            sha256Checksum = unpacked.computedSha256,
            isEncrypted = true,
            detailsJson = textContent ?: "",
            timestamp = System.currentTimeMillis()
        )
        val historyId = transferRepository.saveTransfer(entity)

        ReceivedTransferResult(
            transferId = unpacked.transferId,
            type = unpacked.type,
            title = unpacked.title,
            textContent = textContent,
            contactData = contactPayload,
            savedFiles = savedFiles,
            totalBytes = unpacked.payloadBytes.size.toLong(),
            totalFrames = totalFrames,
            sha256 = unpacked.computedSha256,
            isEncrypted = true,
            transferDurationMs = durationMs,
            savedHistoryId = historyId
        )
    }

    fun reset() {
        chunksMap.clear()
        activeTransferId = null
        activeTotalFrames = 0
        duplicateCount = 0
        frameReceiveTimestamps.clear()
        _uiState.value = ReceiverUiState(status = ReceiverStateStatus.IDLE)
    }

    fun cancelTransfer() {
        chunksMap.clear()
        activeTransferId = null
        _uiState.value = ReceiverUiState(status = ReceiverStateStatus.IDLE)
    }
}
