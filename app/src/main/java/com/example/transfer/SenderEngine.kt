package com.example.transfer

import android.graphics.Bitmap
import com.example.protocol.QRFrame
import com.example.protocol.QRProtocolEngine
import com.example.protocol.TransferManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class SenderUiState(
    val isReady: Boolean = false,
    val manifest: TransferManifest? = null,
    val currentFrameIndex: Int = 1, // 1-based
    val totalFrames: Int = 1,
    val currentFrameString: String = "",
    val currentBitmap: Bitmap? = null,
    val isPlaying: Boolean = true,
    val speedMs: Int = 160,
    val targetMissingFrames: Set<Int> = emptySet(),
    val completedLoops: Int = 0
)

class SenderEngine(private val scope: CoroutineScope) {

    private val _uiState = MutableStateFlow(SenderUiState())
    val uiState: StateFlow<SenderUiState> = _uiState.asStateFlow()

    private var framesList: List<QRFrame> = emptyList()
    private val bitmapCache = ConcurrentHashMap<Int, Bitmap>()
    private var loopJob: Job? = null
    private var currentFilteredIndices: List<Int> = emptyList()
    private var activeIndexPointer: Int = 0

    fun setupTransfer(manifest: TransferManifest, frames: List<QRFrame>, initialSpeedMs: Int = 160) {
        stopLoop()
        bitmapCache.clear()
        framesList = frames
        currentFilteredIndices = frames.map { it.frameIndex }
        activeIndexPointer = 0

        _uiState.value = SenderUiState(
            isReady = true,
            manifest = manifest,
            currentFrameIndex = 1,
            totalFrames = frames.size,
            currentFrameString = if (frames.isNotEmpty()) QRProtocolEngine.encodeFrame(frames[0]) else "",
            isPlaying = true,
            speedMs = initialSpeedMs,
            targetMissingFrames = emptySet(),
            completedLoops = 0
        )

        // Pre-cache first few bitmaps immediately
        scope.launch(Dispatchers.Default) {
            precacheBitmaps(0, minOf(5, frames.size))
            // Render frame 1 immediately
            val initialBitmap = getBitmapForFrame(1)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(currentBitmap = initialBitmap)
            }
            // Pre-cache remainder in background
            precacheBitmaps(0, frames.size)
        }

        startLoop()
    }

    private suspend fun precacheBitmaps(start: Int, end: Int) {
        for (i in start until end) {
            if (i in framesList.indices && !bitmapCache.containsKey(i + 1)) {
                val frame = framesList[i]
                val encoded = QRProtocolEngine.encodeFrame(frame)
                val bmp = QRBitmapGenerator.generateQrBitmap(encoded, sizePx = 512)
                bitmapCache[frame.frameIndex] = bmp
            }
        }
    }

    private suspend fun getBitmapForFrame(frameIndex: Int): Bitmap {
        val cached = bitmapCache[frameIndex]
        if (cached != null) return cached

        val frame = framesList.firstOrNull { it.frameIndex == frameIndex }
        return if (frame != null) {
            val encoded = QRProtocolEngine.encodeFrame(frame)
            val bmp = QRBitmapGenerator.generateQrBitmap(encoded, sizePx = 512)
            bitmapCache[frameIndex] = bmp
            bmp
        } else {
            Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        }
    }

    fun startLoop() {
        loopJob?.cancel()
        if (framesList.isEmpty()) return

        loopJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_uiState.value.isPlaying && currentFilteredIndices.isNotEmpty()) {
                    val frameIndex = currentFilteredIndices[activeIndexPointer]
                    val frame = framesList.firstOrNull { it.frameIndex == frameIndex }
                    val encoded = frame?.let { QRProtocolEngine.encodeFrame(it) } ?: ""
                    val bmp = getBitmapForFrame(frameIndex)

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            currentFrameIndex = frameIndex,
                            currentFrameString = encoded,
                            currentBitmap = bmp
                        )
                    }

                    delay(_uiState.value.speedMs.toLong())

                    activeIndexPointer++
                    if (activeIndexPointer >= currentFilteredIndices.size) {
                        activeIndexPointer = 0
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                completedLoops = _uiState.value.completedLoops + 1
                            )
                        }
                    }
                } else {
                    delay(100)
                }
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    fun togglePlayPause() {
        val newPlaying = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = newPlaying)
    }

    fun stepNext() {
        if (currentFilteredIndices.isEmpty()) return
        activeIndexPointer = (activeIndexPointer + 1) % currentFilteredIndices.size
        val frameIndex = currentFilteredIndices[activeIndexPointer]
        updateSingleFrame(frameIndex)
    }

    fun stepPrev() {
        if (currentFilteredIndices.isEmpty()) return
        activeIndexPointer = (activeIndexPointer - 1 + currentFilteredIndices.size) % currentFilteredIndices.size
        val frameIndex = currentFilteredIndices[activeIndexPointer]
        updateSingleFrame(frameIndex)
    }

    private fun updateSingleFrame(frameIndex: Int) {
        scope.launch(Dispatchers.Default) {
            val frame = framesList.firstOrNull { it.frameIndex == frameIndex }
            val encoded = frame?.let { QRProtocolEngine.encodeFrame(it) } ?: ""
            val bmp = getBitmapForFrame(frameIndex)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    currentFrameIndex = frameIndex,
                    currentFrameString = encoded,
                    currentBitmap = bmp,
                    isPlaying = false
                )
            }
        }
    }

    fun setSpeedMs(speedMs: Int) {
        _uiState.value = _uiState.value.copy(speedMs = speedMs)
    }

    fun filterMissingFrames(missingFrames: Set<Int>) {
        if (missingFrames.isEmpty()) {
            clearMissingFramesFilter()
            return
        }
        val validIndices = missingFrames.filter { idx -> framesList.any { it.frameIndex == idx } }.sorted()
        if (validIndices.isNotEmpty()) {
            currentFilteredIndices = validIndices
            activeIndexPointer = 0
            _uiState.value = _uiState.value.copy(
                targetMissingFrames = validIndices.toSet(),
                isPlaying = true
            )
        }
    }

    fun clearMissingFramesFilter() {
        currentFilteredIndices = framesList.map { it.frameIndex }
        activeIndexPointer = 0
        _uiState.value = _uiState.value.copy(
            targetMissingFrames = emptySet(),
            isPlaying = true
        )
    }

    fun release() {
        stopLoop()
        bitmapCache.clear()
        _uiState.value = SenderUiState()
    }
}
