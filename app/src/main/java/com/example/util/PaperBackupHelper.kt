package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.protocol.QRFrame
import com.example.protocol.QRProtocolEngine
import com.example.protocol.TransferManifest
import com.example.scanner.ImageQRDecoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PaperBackupHelper {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    // 595 x 842 points (8.27 x 11.69 inches)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Generates a printable multi-page Cold Storage PDF document from a TransferManifest and list of QRFrames.
     */
    fun generatePaperBackupPdf(
        context: Context,
        manifest: TransferManifest,
        frames: List<QRFrame>
    ): File {
        val pdfDocument = PdfDocument()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(manifest.timestamp))

        // We can place up to 2 QR codes per A4 page for high scanning fidelity, or 1 if only 1 frame
        val framesPerPage = if (frames.size == 1) 1 else 2
        val totalPages = (frames.size + framesPerPage - 1) / framesPerPage

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 10f
            isAntiAlias = true
        }

        val badgePaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // Emerald
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val bgCardPaint = Paint().apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val monoPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8.5f
            isAntiAlias = true
        }

        var frameCursor = 0

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw Page Header
            canvas.drawRect(30f, 25f, PAGE_WIDTH - 30f, 95f, bgCardPaint)
            canvas.drawRect(30f, 25f, PAGE_WIDTH - 30f, 95f, borderPaint)

            canvas.drawText("DROPQR AIR-GAPPED COLD STORAGE BACKUP", 45f, 48f, titlePaint)
            canvas.drawText(
                "ID: ${manifest.transferId}  •  Type: ${manifest.type.displayName}  •  Date: $formattedDate",
                45f,
                64f,
                subtitlePaint
            )
            canvas.drawText(
                "SHA-256 Hash: ${manifest.overallSha256.take(32)}...  •  Size: ${manifest.totalBytes} Bytes",
                45f,
                78f,
                monoPaint
            )

            // Security Badge
            val badgeRect = RectF(PAGE_WIDTH - 150f, 38f, PAGE_WIDTH - 45f, 58f)
            canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)
            val badgeLabel = if (manifest.isPasswordProtected) "PIN PROTECTED" else "AIR-GAPPED 100%"
            canvas.drawText(badgeLabel, PAGE_WIDTH - 142f, 52f, badgeTextPaint)

            // 2. Draw QR Codes for this page
            val remainingFrames = frames.size - frameCursor
            val countForThisPage = minOf(framesPerPage, remainingFrames)

            val qrAreaTop = 110f
            val qrAreaHeight = if (countForThisPage == 1) 580f else 320f

            for (i in 0 until countForThisPage) {
                val frame = frames[frameCursor]
                val itemTop = qrAreaTop + (i * (qrAreaHeight + 20f))

                // Card background for QR
                val cardRect = RectF(40f, itemTop, PAGE_WIDTH - 40f, itemTop + qrAreaHeight)
                canvas.drawRoundRect(cardRect, 10f, 10f, bgCardPaint)
                canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint)

                // Render High-Resolution QR Bitmap
                val wireString = QRProtocolEngine.encodeFrame(frame)
                val qrSize = if (countForThisPage == 1) 400 else 240
                val qrBitmap = generateQrBitmap(wireString, qrSize)

                if (qrBitmap != null) {
                    val qrLeft = (PAGE_WIDTH - qrSize) / 2f
                    val qrTop = itemTop + 35f
                    canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
                }

                // Frame label header
                val frameLabelPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val label = "Sheet Frame ${frame.frameIndex} of ${manifest.totalFrames}  [CRC-32: ${frame.crc32Hex}]"
                canvas.drawText(label, 55f, itemTop + 24f, frameLabelPaint)

                frameCursor++
            }

            // 3. Footer / Instructions
            val footerTop = PAGE_HEIGHT - 55f
            canvas.drawLine(30f, footerTop - 10f, PAGE_WIDTH - 30f, footerTop - 10f, borderPaint)
            canvas.drawText(
                "Restoration Guide: Open DropQR > Tap Scan > Aim camera at these QR sheets sequentially.",
                40f,
                footerTop + 6f,
                subtitlePaint
            )
            canvas.drawText(
                "Page ${pageIndex + 1} of $totalPages  •  100% Zero-Cloud Cold Storage Document",
                PAGE_WIDTH - 260f,
                footerTop + 20f,
                monoPaint
            )

            pdfDocument.finishPage(page)
        }

        // Save PDF to cache / storage
        val outputDir = File(context.filesDir, "cold_storage_backups").apply { mkdirs() }
        val safeTitle = manifest.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
        val pdfFile = File(outputDir, "DropQR_Backup_${manifest.transferId}_$safeTitle.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        // Also export copy to Downloads so user can access it anytime
        try {
            MediaExportHelper.saveFileToDevice(
                context = context,
                sourceFile = pdfFile,
                originalFileName = pdfFile.name,
                mimeType = "application/pdf"
            )
        } catch (_: Exception) {}

        return pdfFile
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "ISO-8859-1"
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Sends the PDF to system print service (Print dialog / Save as PDF).
     */
    fun printPdf(context: Context, pdfFile: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = PdfPrintAdapter(pdfFile)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build()
                printManager.print("DropQR_Backup_${pdfFile.nameWithoutExtension}", printAdapter, printAttributes)
            } else {
                openPdfWithSystem(context, pdfFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            openPdfWithSystem(context, pdfFile)
        }
    }

    fun openPdfWithSystem(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Cold Storage Backup PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Saved to Downloads/DropQR: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        }
    }
}

class PdfPrintAdapter(private val file: File) : android.print.PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = android.print.PrintDocumentInfo.Builder(file.name)
            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>?,
        destination: android.os.ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("No destination file descriptor")
            return
        }
        try {
            java.io.FileInputStream(file).use { input ->
                java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.localizedMessage)
        }
    }
}
