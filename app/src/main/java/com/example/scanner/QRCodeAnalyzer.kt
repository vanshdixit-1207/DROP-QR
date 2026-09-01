package com.example.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QRCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    private var lastScannedString: String? = null
    private var lastScannedTime: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val planes = imageProxy.planes
                val yBuffer: ByteBuffer = planes[0].buffer
                val ySize = yBuffer.remaining()
                val data = ByteArray(ySize)
                yBuffer.get(data)

                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    data,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )

                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(binaryBitmap)
                val text = result.text

                val now = System.currentTimeMillis()
                // Throttle identical consecutive frames by 50ms to prevent spamming while allowing high FPS
                if (text != lastScannedString || (now - lastScannedTime > 50)) {
                    lastScannedString = text
                    lastScannedTime = now
                    onQrCodeScanned(text)
                }
            } catch (_: NotFoundException) {
                // Expected when no QR in frame
            } catch (_: Exception) {
                // Other parsing exceptions
            } finally {
                reader.reset()
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}
