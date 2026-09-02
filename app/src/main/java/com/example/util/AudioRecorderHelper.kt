package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var isRecording: Boolean = false

    fun startRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "audio_memos").apply { mkdirs() }
            val outputFile = File(audioDir, "memo_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(16000)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording: ${e.message}", e)
            cleanup()
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to stop recording: ${e.message}", e)
            cleanup()
            null
        }
    }

    fun getRecordingDurationMs(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else 0L
    }

    fun isRecordingNow(): Boolean = isRecording

    fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
    }
}
