package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.navigation.DropQRAppNavHost
import com.example.ui.theme.DropQRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as DropQRApplication
        val preferencesRepository = app.preferencesRepository

        setContent {
            val preferences by preferencesRepository.preferencesFlow.collectAsState()
            val isDark = when (preferences.darkModePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            DropQRTheme(darkTheme = isDark) {
                DropQRAppNavHost()
            }
        }
    }
}
