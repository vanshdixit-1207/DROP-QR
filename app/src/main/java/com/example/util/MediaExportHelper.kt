package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class MediaCategory(val label: String, val locationName: String) {
    IMAGE("Photo", "Gallery / Pictures"),
    VIDEO("Video", "Gallery / Movies"),
    AUDIO("Audio", "Music / Audio"),
    DOCUMENT("Document / File", "Downloads")
}

data class ExportResult(
    val isSuccess: Boolean,
    val message: String,
    val savedUri: Uri? = null,
    val savedPath: String? = null,
    val category: MediaCategory = MediaCategory.DOCUMENT
)

object MediaExportHelper {

    fun determineCategory(fileName: String, mimeType: String): MediaCategory {
        val lowerMime = mimeType.lowercase()
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when {
            lowerMime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg") -> {
                MediaCategory.IMAGE
            }
            lowerMime.startsWith("video/") || extension in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm", "flv", "m4v") -> {
                MediaCategory.VIDEO
            }
            lowerMime.startsWith("audio/") || extension in listOf("mp3", "m4a", "wav", "aac", "ogg", "flac", "opus", "amr", "m4b") -> {
                MediaCategory.AUDIO
            }
            else -> {
                MediaCategory.DOCUMENT
            }
        }
    }

    fun resolveMimeType(fileName: String, existingMime: String): String {
        if (existingMime.isNotBlank() && existingMime != "application/octet-stream" && existingMime != "*/*") {
            return existingMime
        }
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty()) {
            val fromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!fromExt.isNullOrBlank()) {
                return fromExt
            }
        }
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    fun saveFileToDevice(
        context: Context,
        sourceFile: File,
        originalFileName: String,
        mimeType: String
    ): ExportResult {
        if (!sourceFile.exists()) {
            return ExportResult(
                isSuccess = false,
                message = "Source file not found",
                category = MediaCategory.DOCUMENT
            )
        }

        val safeFileName = if (originalFileName.isNotBlank()) {
            originalFileName
        } else {
            sourceFile.name.substringAfter('_', sourceFile.name)
        }

        val resolvedMime = resolveMimeType(safeFileName, mimeType)
        val category = determineCategory(safeFileName, resolvedMime)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, sourceFile, safeFileName, resolvedMime, category)
            } else {
                saveWithLegacyStorage(context, sourceFile, safeFileName, resolvedMime, category)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                isSuccess = false,
                message = "Failed to save: ${e.localizedMessage ?: "Unknown error"}",
                category = category
            )
        }
    }

    private fun saveWithMediaStore(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String,
        category: MediaCategory
    ): ExportResult {
        val resolver = context.contentResolver

        val (contentUri, relativePath) = when (category) {
            MediaCategory.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "Pictures/DropQR"
            MediaCategory.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "Movies/DropQR"
            MediaCategory.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to "Music/DropQR"
            MediaCategory.DOCUMENT -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to "Download/DropQR"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(contentUri, contentValues)
            ?: return saveWithLegacyStorage(context, sourceFile, fileName, mimeType, category)

        try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("Could not open output stream for MediaStore Uri")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            val locationMsg = when (category) {
                MediaCategory.IMAGE, MediaCategory.VIDEO -> "Saved to Gallery ($relativePath/$fileName)"
                MediaCategory.AUDIO -> "Saved to Music ($relativePath/$fileName)"
                MediaCategory.DOCUMENT -> "Saved to Downloads ($relativePath/$fileName)"
            }

            // Trigger MediaScanner for instant gallery indexing
            MediaScannerConnection.scanFile(
                context,
                arrayOf(sourceFile.absolutePath),
                arrayOf(mimeType),
                null
            )

            return ExportResult(
                isSuccess = true,
                message = locationMsg,
                savedUri = uri,
                category = category
            )
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun saveWithLegacyStorage(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String,
        category: MediaCategory
    ): ExportResult {
        val publicDirType = when (category) {
            MediaCategory.IMAGE -> Environment.DIRECTORY_PICTURES
            MediaCategory.VIDEO -> Environment.DIRECTORY_MOVIES
            MediaCategory.AUDIO -> Environment.DIRECTORY_MUSIC
            MediaCategory.DOCUMENT -> Environment.DIRECTORY_DOWNLOADS
        }

        val targetDir = File(
            Environment.getExternalStoragePublicDirectory(publicDirType),
            "DropQR"
        )
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = File(targetDir, fileName)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        // Notify MediaScanner so photos/videos immediately show in Gallery / Google Photos
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
            // scanned
        }

        val locationMsg = when (category) {
            MediaCategory.IMAGE, MediaCategory.VIDEO -> "Saved to Gallery (${targetDir.name}/$fileName)"
            MediaCategory.AUDIO -> "Saved to Music (${targetDir.name}/$fileName)"
            MediaCategory.DOCUMENT -> "Saved to Downloads (${targetDir.name}/$fileName)"
        }

        return ExportResult(
            isSuccess = true,
            message = locationMsg,
            savedPath = targetFile.absolutePath,
            category = category
        )
    }

    fun openInSystemViewer(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val resolvedMime = resolveMimeType(file.name, mimeType)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
