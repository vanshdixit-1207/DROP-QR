package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DropQRDarkColorScheme = darkColorScheme(
    primary = BentoPrimaryBlueDark,
    onPrimary = Color(0xFF001D35),
    primaryContainer = BentoSkyDark,
    onPrimaryContainer = BentoPrimaryBlueDark,
    secondary = BentoCyan,
    onSecondary = Color(0xFF001D35),
    secondaryContainer = BentoCyanDark,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = BentoLavender,
    onTertiary = BentoLavenderDark,
    background = BentoBgDark,
    onBackground = BentoTextPrimaryDark,
    surface = BentoSurfaceDark,
    onSurface = BentoTextPrimaryDark,
    surfaceVariant = Color(0xFF192A44),
    onSurfaceVariant = BentoTextSecondaryDark,
    outline = BentoCardBorderDark,
    outlineVariant = Color(0x1FFFFFFF),
    error = CoralRed,
    onError = Color.White
)

private val DropQRLightColorScheme = lightColorScheme(
    primary = BentoPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = BentoSky,
    onPrimaryContainer = BentoSkyText,
    secondary = BentoCyan,
    onSecondary = BentoCyanText,
    secondaryContainer = BentoCyan,
    onSecondaryContainer = BentoCyanText,
    tertiary = BentoLavender,
    onTertiary = BentoLavenderText,
    background = BentoBgLight,
    onBackground = BentoTextPrimaryLight,
    surface = BentoSurfaceLight,
    onSurface = BentoTextPrimaryLight,
    surfaceVariant = Color(0xFFEAEFF8),
    onSurfaceVariant = BentoTextSecondaryLight,
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1),
    error = CoralRed,
    onError = Color.White
)

@Composable
fun DropQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DropQRDarkColorScheme else DropQRLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
