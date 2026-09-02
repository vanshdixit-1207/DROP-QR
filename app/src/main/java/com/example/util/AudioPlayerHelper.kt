package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    fun playFile(file: File, onCompletion: (() -> Unit)? = null) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    onCompletion?.invoke()
                }
                start()
            }
            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play audio file: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause(file: File) {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else if (player != null) {
            player.start()
            _isPlaying.value = true
        } else {
            playFile(file)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    fun release() {
        stop()
    }
}
