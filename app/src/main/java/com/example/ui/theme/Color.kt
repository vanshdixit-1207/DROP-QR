package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Bento Grid Palette (Light Mode)
val BentoBgLight = Color(0xFFF6F8FC)
val BentoSurfaceLight = Color(0xFFFFFFFF)
val BentoSky = Color(0xFFD3E3FD)           // Hero Sky Blue tile
val BentoSkyText = Color(0xFF041E49)
val BentoCyan = Color(0xFFC2E7FF)          // Secondary Light Blue tile
val BentoCyanText = Color(0xFF001D35)
val BentoLavender = Color(0xFFE7E0FF)      // Purple/Lavender tile
val BentoLavenderText = Color(0xFF381E72)
val BentoDarkNavy = Color(0xFF001D35)      // Deep Navy Dark tile
val BentoDarkNavyText = Color(0xFFFFFFFF)
val BentoOrange = Color(0xFFFFEDD5)        // Warm Amber/Orange tile
val BentoOrangeText = Color(0xFF7C2D12)
val BentoEmerald = Color(0xFFD1FAE5)       // Mint/Emerald tile
val BentoEmeraldText = Color(0xFF065F46)
val BentoCardBorderLight = Color(0x33001D35)

// Bento Grid Palette (Dark Mode)
val BentoBgDark = Color(0xFF0B1320)
val BentoSurfaceDark = Color(0xFF131F33)
val BentoSkyDark = Color(0xFF162A4A)
val BentoCyanDark = Color(0xFF103652)
val BentoLavenderDark = Color(0xFF281C45)
val BentoDarkNavyDark = Color(0xFF081326)
val BentoOrangeDark = Color(0xFF3B2314)
val BentoEmeraldDark = Color(0xFF0D3327)
val BentoCardBorderDark = Color(0x2EFFFFFF)

// Accent Colors
val BentoPrimaryBlue = Color(0xFF0B57D0)
val BentoPrimaryBlueDark = Color(0xFFA8C7FA)
val BentoActivePill = Color(0xFFD3E3FD)
val BentoActivePillDark = Color(0xFF1D3B66)

// Text Colors
val BentoTextPrimaryLight = Color(0xFF1E293B)
val BentoTextSecondaryLight = Color(0xFF64748B)
val BentoTextTertiaryLight = Color(0xFF94A3B8)

val BentoTextPrimaryDark = Color(0xFFF1F5F9)
val BentoTextSecondaryDark = Color(0xFF94A3B8)
val BentoTextTertiaryDark = Color(0xFF64748B)

// Gradients & Specular Brushes
val BentoBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1A73E8),
        Color(0xFF0B57D0)
    )
)

val BentoCyanBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF00A3FF),
        Color(0xFF0066FF)
    )
)

val BentoEmeraldGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF059669),
        Color(0xFF10B981)
    )
)

val BentoPurpleGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7C3AED),
        Color(0xFF9333EA)
    )
)

// Legacy alias compatibility to maintain seamless build
val DarkBackground = BentoBgDark
val DarkSurface = BentoSurfaceDark
val DarkCardSurface = Color(0xFF17253D)
val DarkCardBorder = Color(0x2EFFFFFF)

val LightBackground = BentoBgLight
val LightSurface = BentoSurfaceLight
val LightCardSurface = Color(0xFFFFFFFF)
val LightCardBorder = Color(0x1F0F172A)

val AppleBlue = BentoPrimaryBlue
val AppleBlueDark = BentoPrimaryBlueDark
val ElectricCyan = Color(0xFF00B4D8)
val CyanAccent = Color(0xFF0284C7)
val EmeraldGreen = Color(0xFF10B981)
val EmeraldGreenDark = Color(0xFF34D399)
val AmberWarning = Color(0xFFEA580C)
val CoralRed = Color(0xFFEF4444)
val PurpleSecurity = Color(0xFF7C3AED)
val IndigoVision = Color(0xFF4F46E5)

val TextPrimaryDark = BentoTextPrimaryDark
val TextSecondaryDark = BentoTextSecondaryDark
val TextTertiaryDark = BentoTextTertiaryDark

val TextPrimaryLight = BentoTextPrimaryLight
val TextSecondaryLight = BentoTextSecondaryLight
val TextTertiaryLight = BentoTextTertiaryLight

val CyanBlueGradient = BentoCyanBlueGradient
val GlassBorderDark = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.05f)
    )
)
val GlassBorderLight = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.90f),
        Color(0xFFCBD5E1).copy(alpha = 0.60f)
    )
)
