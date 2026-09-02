package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity

@Composable
fun ChunkMatrixGrid(
    totalChunks: Int,
    receivedChunks: Set<Int>,
    currentActiveIndex: Int? = null,
    modifier: Modifier = Modifier,
    title: String = "Transmission Chunk Matrix",
    onChunkClick: ((Int) -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val progressPercent = if (totalChunks > 0) ((receivedChunks.size.toFloat() / totalChunks) * 100).toInt() else 0

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (isDark) Color(0x900D1527) else Color(0x90F1F5F9),
        borderBrush = Brush.linearGradient(
            listOf(
                ElectricCyan.copy(alpha = 0.35f),
                PurpleSecurity.copy(alpha = 0.2f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${receivedChunks.size}/$totalChunks",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$progressPercent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }

            // Chunk Grid
            val displayLimit = minOf(totalChunks, 60)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(totalChunks) { index ->
                    val frameNum = index + 1
                    val isReceived = receivedChunks.contains(frameNum)
                    val isActive = currentActiveIndex == frameNum

                    val bgColor by animateColorAsState(
                        targetValue = when {
                            isActive -> ElectricCyan
                            isReceived -> EmeraldGreen.copy(alpha = 0.85f)
                            else -> if (isDark) Color(0x20FFFFFF) else Color(0x18000000)
                        },
                        label = "chunk_bg"
                    )

                    val borderColor = when {
                        isActive -> ElectricCyan
                        isReceived -> EmeraldGreen
                        else -> if (isDark) Color(0x30FFFFFF) else Color(0x25000000)
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(if (isActive) pulseScale else 1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = onChunkClick != null) {
                                onChunkClick?.invoke(frameNum)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReceived && !isActive && totalChunks > 30) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "$frameNum",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isActive || isReceived) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive || isReceived) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = EmeraldGreen, label = "Received/Done")
                LegendItem(color = ElectricCyan, label = "Active Beam")
                LegendItem(color = if (isDark) Color(0x40FFFFFF) else Color(0x40000000), label = "Pending")
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
