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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToSend: (TransferPayloadType?) -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSteganography: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header (Clean DropQR branding + 100% Offline Air-Gapped status)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AIR-GAPPED OFFLINE TRANSFER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DropQR",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = if (isDark) BentoTextPrimaryDark else BentoTextPrimaryLight
                    )
                }

                AirGapBadge()
            }
        }

        // 2. Primary Action 1: SEND (Hero Bento Card)
        item {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = if (isDark) BentoSkyDark else BentoSky,
                elevation = 3.dp,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BentoPillBadge(
                            text = "SEND DATA",
                            backgroundColor = if (isDark) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                            textColor = if (isDark) BentoPrimaryBlueDark else BentoSkyText,
                            icon = Icons.Default.Send
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send",
                                tint = if (isDark) Color(0xFF001D35) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Send via QR Code",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else BentoSkyText
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Encode notes, links, or files into animated QR frames",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF1E3A8A).copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 3. Primary Action 2: RECEIVE (Emerald Hero Bento Card)
        item {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = if (isDark) BentoEmeraldDark else BentoEmerald,
                elevation = 3.dp,
                onClick = onNavigateToReceive,
                testTag = "home_receive_card"
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BentoPillBadge(
                            text = "RECEIVE DATA",
                            backgroundColor = if (isDark) Color(0x33FFFFFF) else Color(0x80FFFFFF),
                            textColor = if (isDark) EmeraldGreen else BentoEmeraldText,
                            icon = Icons.Default.QrCodeScanner
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) EmeraldGreen else Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Scan & Receive",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else BentoEmeraldText
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Open camera scanner to read & reconstruct incoming data",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFA7F3D0) else BentoEmeraldText.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 3.5 Primary Action 3: Steganography (Ghost Mode Card)
        item {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                elevation = 2.dp,
                onClick = onNavigateToSteganography,
                testTag = "home_stego_card"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        BentoPillBadge(
                            text = "GHOST MODE",
                            backgroundColor = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000),
                            textColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            icon = Icons.Default.VisibilityOff
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Optical Steganography",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF334155)
                        )
                        Text(
                            text = "Hide secret files invisibly inside normal photos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Ghost Mode",
                            tint = if (isDark) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // History Card
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(24.dp),
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = if (isDark) Color(0xFFFB923C) else BentoOrangeText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "History",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoOrangeText
                            )
                            Text(
                                text = "Transfer Logs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFFDBA74) else BentoOrangeText.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Settings Card
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = if (isDark) BentoLavenderDark else BentoLavender,
                    elevation = 2.dp,
                    onClick = onNavigateToSettings,
                    testTag = "home_settings_card"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33FFFFFF) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isDark) PurpleSecurity else BentoLavenderText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Settings",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else BentoLavenderText
                            )
                            Text(
                                text = "Speed & Security",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFDDD6FE) else BentoLavenderText.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Security Guarantee Banner
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = if (isDark) BentoSurfaceDark else BentoSurfaceLight,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) BentoLavenderDark else BentoLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isDark) PurpleSecurity else BentoLavenderText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AES-256 Air-Gapped Protocol",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zero internet, Wi-Fi, or Bluetooth sockets used. Pure visual light transmission.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
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
