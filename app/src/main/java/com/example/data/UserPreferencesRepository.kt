package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val frameSpeedMs: Int = 160,
    val chunkSizeBytes: Int = 320,
    val keepScreenAwake: Boolean = true,
    val maxBrightness: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val compressionEnabled: Boolean = true,
    val requireConfirmationBeforeSave: Boolean = false,
    val autoDeleteTempData: Boolean = true,
    val darkModePreference: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    val onboardingCompleted: Boolean = false
)

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dropqr_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<UserPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            frameSpeedMs = prefs.getInt("frame_speed_ms", 160),
            chunkSizeBytes = prefs.getInt("chunk_size_bytes", 320),
            keepScreenAwake = prefs.getBoolean("keep_screen_awake", true),
            maxBrightness = prefs.getBoolean("max_brightness", false),
            encryptionEnabled = prefs.getBoolean("encryption_enabled", true),
            compressionEnabled = prefs.getBoolean("compression_enabled", true),
            requireConfirmationBeforeSave = prefs.getBoolean("require_confirm_save", false),
            autoDeleteTempData = prefs.getBoolean("auto_delete_temp", true),
            darkModePreference = prefs.getString("dark_mode_pref", "SYSTEM") ?: "SYSTEM",
            onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        )
    }

    fun setFrameSpeedMs(speedMs: Int) {
        prefs.edit().putInt("frame_speed_ms", speedMs).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(frameSpeedMs = speedMs)
    }

    fun setChunkSizeBytes(chunkSize: Int) {
        prefs.edit().putInt("chunk_size_bytes", chunkSize).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(chunkSizeBytes = chunkSize)
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        prefs.edit().putBoolean("keep_screen_awake", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(keepScreenAwake = enabled)
    }

    fun setMaxBrightness(enabled: Boolean) {
        prefs.edit().putBoolean("max_brightness", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(maxBrightness = enabled)
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("encryption_enabled", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(encryptionEnabled = enabled)
    }

    fun setCompressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("compression_enabled", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(compressionEnabled = enabled)
    }

    fun setRequireConfirmationBeforeSave(enabled: Boolean) {
        prefs.edit().putBoolean("require_confirm_save", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(requireConfirmationBeforeSave = enabled)
    }

    fun setAutoDeleteTempData(enabled: Boolean) {
        prefs.edit().putBoolean("auto_delete_temp", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(autoDeleteTempData = enabled)
    }

    fun setDarkModePreference(mode: String) {
        prefs.edit().putString("dark_mode_pref", mode).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(darkModePreference = mode)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(onboardingCompleted = completed)
    }
}
