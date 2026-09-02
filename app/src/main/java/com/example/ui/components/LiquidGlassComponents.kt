package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoBgDark
import com.example.ui.theme.BentoBgLight
import com.example.ui.theme.BentoBlueGradient
import com.example.ui.theme.BentoCardBorderDark
import com.example.ui.theme.BentoCardBorderLight
import com.example.ui.theme.BentoCyan
import com.example.ui.theme.BentoCyanDark
import com.example.ui.theme.BentoCyanText
import com.example.ui.theme.BentoDarkNavy
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoEmeraldDark
import com.example.ui.theme.BentoEmeraldText
import com.example.ui.theme.BentoLavender
import com.example.ui.theme.BentoLavenderDark
import com.example.ui.theme.BentoLavenderText
import com.example.ui.theme.BentoOrange
import com.example.ui.theme.BentoOrangeDark
import com.example.ui.theme.BentoOrangeText
import com.example.ui.theme.BentoPrimaryBlue
import com.example.ui.theme.BentoPrimaryBlueDark
import com.example.ui.theme.BentoSky
import com.example.ui.theme.BentoSkyDark
import com.example.ui.theme.BentoSkyText
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.BentoSurfaceLight
import com.example.ui.theme.BentoTextPrimaryDark
import com.example.ui.theme.BentoTextPrimaryLight
import com.example.ui.theme.BentoTextSecondaryDark
import com.example.ui.theme.BentoTextSecondaryLight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) BentoBgDark else BentoBgLight)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (isDark) {
                // Subtle Bento Deep Glows
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF162A4A).copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.90f, canvasHeight * 0.10f),
                        radius = canvasWidth * 0.70f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF281C45).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.10f, canvasHeight * 0.85f),
                        radius = canvasWidth * 0.65f
                    )
                )
            } else {
                // Bento Clean Slate with soft sky & lavender ambient tints
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BentoSky.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.90f, canvasHeight * 0.08f),
                        radius = canvasWidth * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BentoLavender.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.10f, canvasHeight * 0.80f),
                        radius = canvasWidth * 0.60f
                    )
                )
            }
        }

        content()
    }
}

/**
 * Bento Grid Tile Card
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color? = null,
    borderStroke: BorderStroke? = null,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "bento_card",
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDark.current
    val defaultBg = backgroundColor ?: if (isDark) BentoSurfaceDark else BentoSurfaceLight
    val defaultBorder = borderStroke ?: BorderStroke(
        1.dp,
        if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
    )

    val baseModifier = modifier
        .testTag(testTag)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF0F172A).copy(alpha = 0.04f),
            spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.06f)
        )
        .clip(shape)
        .background(defaultBg)
        .border(defaultBorder, shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        content()
    }
}

// Retain GlassCard as alias for BentoCard for backwards compatibility
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "glass_card",
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDark.current
    val defaultBg = backgroundColor ?: if (isDark) BentoSurfaceDark else BentoSurfaceLight
    val stroke = if (borderBrush != null) {
        BorderStroke(1.dp, borderBrush)
    } else {
        BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
    }

    BentoCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = defaultBg,
        borderStroke = stroke,
        elevation = elevation,
        onClick = onClick,
        testTag = testTag,
        content = content
    )
}

/**
 * Bento Grid Pill Badge (e.g. Next Up, Air-Gapped, AES-256)
 */
@Composable
fun BentoPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    icon: ImageVector? = null
) {
    val isDark = LocalIsDark.current
    val bg = backgroundColor ?: if (isDark) Color(0x33FFFFFF) else Color(0x80FFFFFF)
    val txt = textColor ?: if (isDark) Color.White else BentoPrimaryBlue

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                BorderStroke(
                    1.dp,
                    if (isDark) Color(0x2EFFFFFF) else Color(0x33FFFFFF)
                ),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = txt,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = txt
            )
        }
    }
}

@Composable
fun BentoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String = "bento_button"
) {
    val isDark = LocalIsDark.current

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue,
                contentColor = if (isDark) Color(0xFF001D35) else Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
            modifier = modifier.testTag(testTag)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isDark) Color(0xFF001D35) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    color = if (isDark) Color(0xFF001D35) else Color.White
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .testTag(testTag)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) BentoSurfaceDark else BentoSurfaceLight)
                .border(
                    BorderStroke(1.dp, if (isDark) Color(0x2EFFFFFF) else Color(0xFFE2E8F0)),
                    RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Retain GlassButton as alias
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String = "glass_button"
) {
    BentoButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        isPrimary = isPrimary,
        enabled = enabled,
        testTag = testTag
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = "glass_icon_button"
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = modifier
            .testTag(testTag)
            .size(size)
            .clip(CircleShape)
            .background(if (isDark) BentoSurfaceDark else BentoSurfaceLight)
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AirGapBadge(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isDark) BentoEmeraldDark else BentoEmerald)
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x3310B981) else Color(0x4010B981)),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isDark) EmeraldGreen else BentoEmeraldText)
            )
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isDark) EmeraldGreen else BentoEmeraldText,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "100% OFFLINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = if (isDark) EmeraldGreen else BentoEmeraldText
            )
        }
    }
}

@Composable
fun EncryptionBadge(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isDark) BentoLavenderDark else BentoLavender)
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x337C3AED) else Color(0x407C3AED)),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isDark) PurpleSecurity else BentoLavenderText,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "AES-256-GCM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = if (isDark) PurpleSecurity else BentoLavenderText
            )
        }
    }
}

@Composable
fun GlassSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF131F33) else Color(0xFFEAEFF8))
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1).copy(alpha = 0.5f)),
                RoundedCornerShape(18.dp)
            )
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(if (isDark) BentoPrimaryBlueDark else Color.White)
                                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (isDark) Color(0xFF001D35) else BentoPrimaryBlue
                        } else {
                            if (isDark) BentoTextSecondaryDark else BentoTextSecondaryLight
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassProgressBar(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    brush: Brush = BentoBlueGradient,
    height: Dp = 8.dp
) {
    val isDark = LocalIsDark.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300, easing = LinearEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(if (isDark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(brush)
        )
    }
}
