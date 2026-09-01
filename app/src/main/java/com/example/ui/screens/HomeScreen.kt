package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.TransferPayloadType
import com.example.ui.components.AirGapBadge
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillBadge
import com.example.ui.theme.BentoBlueGradient
import com.example.ui.theme.BentoCyan
import com.example.ui.theme.BentoCyanDark
import com.example.ui.theme.BentoCyanText
import com.example.ui.theme.BentoDarkNavy
import com.example.ui.theme.BentoDarkNavyDark
import com.example.ui.theme.BentoDarkNavyText
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
import com.example.ui.theme.PurpleSecurity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToSend: (TransferPayloadType?) -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bento Header (Greeting & Avatar Badge)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateStr.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = if (isDark) BentoTextSecondaryDark else BentoTextSecondaryLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DropQR Air-Gap",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = if (isDark) BentoTextPrimaryDark else BentoTextPrimaryLight
                    )
                }

                // Avatar / Air-Gap Status Indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) BentoSkyDark else BentoSky)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onNavigateToSettings
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline Mode",
                        tint = if (isDark) BentoPrimaryBlueDark else BentoSkyText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Bento Hero Tile 1: [SEND AIR-GAPPED DATA] (Span 2)
        item {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = if (isDark) BentoSkyDark else BentoSky,
                elevation = 2.dp,
                onClick = { onNavigateToSend(null) },
                testTag = "home_send_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        BentoPillBadge(
                            text = "Next Up • Send",
                            backgroundColor = if (isDark) Color(0x33FFFFFF) else Color(0x70FFFFFF),
                            textColor = if (isDark) BentoPrimaryBlueDark else BentoSkyText
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Broadcast Data Stream",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else BentoSkyText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Encode files & notes into high-speed animated QR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) BentoSkyText.copy(alpha = 0.8f) else Color(0xFF1E3A8A).copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 3. Bento 2-Column Grid (Row 1): [Receive & Scan] (Col 1) + [Security AES-256] (Col 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receive & Scan Tile (Emerald / Mint Bento)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = if (isDark) BentoEmeraldDark else BentoEmerald,
                    elevation = 2.dp,
                    onClick = onNavigateToReceive,
                    testTag = "home_receive_card"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = if (isDark) EmeraldGreen else BentoEmeraldText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Receive",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) EmeraldGreen else BentoEmeraldText
                            )
                        }

                        Column {
                            Text(
                                text = "Scan QR",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoEmeraldText
                            )
                            Text(
                                text = "Live Viewfinder",
                                fontSize = 12.sp,
                                color = if (isDark) EmeraldGreen.copy(alpha = 0.8f) else BentoEmeraldText.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Security & Privacy Tile (Lavender Bento)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = if (isDark) BentoLavenderDark else BentoLavender,
                    elevation = 2.dp,
                    onClick = onNavigateToSettings,
                    testTag = "home_security_card"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isDark) PurpleSecurity else BentoLavenderText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Security",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) PurpleSecurity else BentoLavenderText
                            )
                        }

                        Column {
                            Text(
                                text = "AES-256",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoLavenderText
                            )
                            Text(
                                text = "GCM Encrypted",
                                fontSize = 12.sp,
                                color = if (isDark) PurpleSecurity.copy(alpha = 0.8f) else BentoLavenderText.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bento 2-Column Grid (Row 2): [Speed Tuning] (Col 1) + [Transfer History] (Col 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Speed Tuning Tile (Cyan Bento)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = if (isDark) BentoCyanDark else BentoCyan,
                    elevation = 2.dp,
                    onClick = onNavigateToSettings,
                    testTag = "home_speed_card"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isDark) ElectricCyan else BentoCyanText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Engine",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ElectricCyan else BentoCyanText
                            )
                        }

                        Column {
                            Text(
                                text = "120 ms",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoCyanText
                            )
                            Text(
                                text = "~8.3 FPS Rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ElectricCyan.copy(alpha = 0.8f) else BentoCyanText.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // History & Logs Tile (Warm Orange Bento)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = if (isDark) BentoOrangeDark else BentoOrange,
                    elevation = 2.dp,
                    onClick = onNavigateToHistory,
                    testTag = "home_history_card"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFB923C) else BentoOrangeText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "History",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFB923C) else BentoOrangeText
                            )
                        }

                        Column {
                            Text(
                                text = "Logs",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoOrangeText
                            )
                            Text(
                                text = "View All Items",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFB923C).copy(alpha = 0.8f) else BentoOrangeText.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Bento Hero Tile 3: [DEEP NAVY QUICK SEND HUB] (Span 2)
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = if (isDark) BentoDarkNavyDark else BentoDarkNavy,
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Text(
                                text = "QUICK SEND HUB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Select Type",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // 3-Column Bento Inner Tiles Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoDarkQuickTile(
                            title = "Note",
                            icon = Icons.Default.Notes,
                            iconBg = Color(0xFF1E3A8A),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.TEXT) }
                        )
                        BentoDarkQuickTile(
                            title = "Link",
                            icon = Icons.Default.Link,
                            iconBg = Color(0xFF065F46),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.URL) }
                        )
                        BentoDarkQuickTile(
                            title = "Contact",
                            icon = Icons.Default.Contacts,
                            iconBg = Color(0xFF581C87),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.CONTACT) }
                        )
                    }

                    // 3-Column Bento Inner Tiles Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoDarkQuickTile(
                            title = "Photos",
                            icon = Icons.Default.Image,
                            iconBg = Color(0xFF831843),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.FILE) }
                        )
                        BentoDarkQuickTile(
                            title = "Docs",
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFF7C2D12),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.FILE) }
                        )
                        BentoDarkQuickTile(
                            title = "Files",
                            icon = Icons.Default.Folder,
                            iconBg = Color(0xFF1E1B4B),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToSend(TransferPayloadType.MULTI_FILE) }
                        )
                    }
                }
            }
        }

        // 6. Bento Manifesto Card (Span 2)
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = if (isDark) BentoSurfaceDark else BentoSurfaceLight,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) BentoLavenderDark else BentoLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isDark) PurpleSecurity else BentoLavenderText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zero Cloud • Air-Gapped",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Transfers happen entirely via visual light without Internet, Bluetooth or Wi-Fi sockets.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BentoDarkQuickTile(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
