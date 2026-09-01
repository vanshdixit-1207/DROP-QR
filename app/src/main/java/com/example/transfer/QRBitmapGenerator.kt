package com.example.transfer

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

object QRBitmapGenerator {

    private val qrWriter = QRCodeWriter()
    private val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.CHARACTER_SET, "ISO-8859-1")
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        put(EncodeHintType.MARGIN, 2)
    }

    suspend fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val bitMatrix = qrWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) foreground else background
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            // Fallback empty white bitmap on unexpected encoding error
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565).apply {
                eraseColor(background)
            }
        }
    }
}
