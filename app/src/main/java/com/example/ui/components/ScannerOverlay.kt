package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanBlueGradient
import com.example.ui.theme.ElectricCyan

@Composable
fun ScannerOverlay(
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onPickImage: () -> Unit,
    onClose: () -> Unit,
    statusText: String = "Align QR code inside the frame",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Darkened mask outside viewfinder
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val boxSize = minOf(canvasWidth * 0.76f, 320.dp.toPx())
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2.3f

            // Full translucent black overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                size = Size(canvasWidth, canvasHeight)
            )

            // Punch out viewfinder window
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, left + boxSize, top + boxSize),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                )
            }
            drawPath(path, Color.Transparent, blendMode = BlendMode.Clear)

            // Draw Corner Brackets
            val cornerLen = 32.dp.toPx()
            val strokeW = 4.5.dp.toPx()
            val cornerRad = 16.dp.toPx()
            val cornerColor = ElectricCyan

            // Top-Left Corner
            val tlPath = Path().apply {
                moveTo(left, top + cornerLen)
                lineTo(left, top + cornerRad)
                quadraticTo(left, top, left + cornerRad, top)
                lineTo(left + cornerLen, top)
            }
            drawPath(tlPath, cornerColor, style = Stroke(strokeW, cap = StrokeCap.Round))

            // Top-Right Corner
            val trPath = Path().apply {
                moveTo(left + boxSize - cornerLen, top)
                lineTo(left + boxSize - cornerRad, top)
                quadraticTo(left + boxSize, top, left + boxSize, top + cornerRad)
                lineTo(left + boxSize, top + cornerLen)
            }
            drawPath(trPath, cornerColor, style = Stroke(strokeW, cap = StrokeCap.Round))

            // Bottom-Left Corner
            val blPath = Path().apply {
                moveTo(left, top + boxSize - cornerLen)
                lineTo(left, top + boxSize - cornerRad)
                quadraticTo(left, top + boxSize, left + cornerRad, top + boxSize)
                lineTo(left + cornerLen, top + boxSize)
            }
            drawPath(blPath, cornerColor, style = Stroke(strokeW, cap = StrokeCap.Round))

            // Bottom-Right Corner
            val brPath = Path().apply {
                moveTo(left + boxSize - cornerLen, top + boxSize)
                lineTo(left + boxSize - cornerRad, top + boxSize)
                quadraticTo(left + boxSize, top + boxSize, left + boxSize, top + boxSize - cornerRad)
                lineTo(left + boxSize, top + boxSize - cornerLen)
            }
            drawPath(brPath, cornerColor, style = Stroke(strokeW, cap = StrokeCap.Round))

            // Sweeping Laser Beam
            val currentLaserY = top + (boxSize * laserPosition)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ElectricCyan.copy(alpha = 0.9f),
                        Color.White,
                        ElectricCyan.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    startX = left + 10f,
                    endX = left + boxSize - 10f
                ),
                start = Offset(left + 10f, currentLaserY),
                end = Offset(left + boxSize - 10f, currentLaserY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close scanner",
                    tint = Color.White
                )
            }

            AirGapBadge()

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Scan from gallery",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) ElectricCyan.copy(alpha = 0.3f) else Color(0x33000000))
                        .border(
                            1.dp,
                            if (isTorchOn) ElectricCyan else Color(0x33FFFFFF),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle flashlight",
                        tint = if (isTorchOn) ElectricCyan else Color.White
                    )
                }
            }
        }

        // Bottom Instruction Card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x80000000))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
