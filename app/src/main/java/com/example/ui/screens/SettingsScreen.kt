package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.UserPreferencesRepository
import com.example.util.DeviceAuthHelper
import com.example.ui.components.AirGapBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSegmentedControl
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    val preferences by preferencesRepository.preferencesFlow.collectAsState()
    var tempCacheSizeStr by remember { mutableStateOf(calculateTempCacheSize(context)) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back"
                )

                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                AirGapBadge()
            }
        }

        // Section 1: Transfer Engine
        item {
            SettingsSectionHeader(title = "Transfer Engine", icon = Icons.Default.Speed)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Frame Speed Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Broadcast Frame Speed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val fps = 1000f / preferences.frameSpeedMs
                            Text(
                                text = "${preferences.frameSpeedMs}ms (~${String.format("%.1f", fps)} fps)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = preferences.frameSpeedMs.toFloat(),
                            onValueChange = { preferencesRepository.setFrameSpeedMs(it.toInt()) },
                            valueRange = 80f..350f,
                            steps = 26,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = AppleBlue
                            )
                        )
                    }

                    // Chunk Size Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "QR Chunk Payload Size",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${preferences.chunkSizeBytes} bytes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = preferences.chunkSizeBytes.toFloat(),
                            onValueChange = { preferencesRepository.setChunkSizeBytes(it.toInt()) },
                            valueRange = 180f..500f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = AppleBlue
                            )
                        )
                    }

                    // Keep Screen Awake Toggle
                    SettingsToggleRow(
                        title = "Keep Screen Awake",
                        subtitle = "Prevent screen timeout during active QR broadcast",
                        checked = preferences.keepScreenAwake,
                        onCheckedChange = { preferencesRepository.setKeepScreenAwake(it) }
                    )

                    // Maximum Brightness Boost Toggle
                    SettingsToggleRow(
                        title = "Max Brightness Mode",
                        subtitle = "Temporarily boost display brightness for easy scanning",
                        checked = preferences.maxBrightness,
                        onCheckedChange = { preferencesRepository.setMaxBrightness(it) }
                    )

                    // Compression Toggle
                    SettingsToggleRow(
                        title = "GZIP Data Compression",
                        subtitle = "Compress payload to reduce total QR frames count",
                        checked = preferences.compressionEnabled,
                        onCheckedChange = { preferencesRepository.setCompressionEnabled(it) }
                    )
                }
            }
        }

        // Section 2: Security & Privacy
        item {
            SettingsSectionHeader(title = "Security & Privacy", icon = Icons.Default.Lock)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsToggleRow(
                        title = "AES-256-GCM Encryption",
                        subtitle = "Encrypt all transferred frames with unique session keys",
                        checked = preferences.encryptionEnabled,
                        onCheckedChange = { preferencesRepository.setEncryptionEnabled(it) }
                    )

                    SettingsToggleRow(
                        title = "Biometric & Device Lock",
                        subtitle = "Use your phone's fingerprint, face unlock, or device PIN/Pattern",
                        checked = preferences.appLockEnabled,
                        onCheckedChange = { enable ->
                            val activity = context as? FragmentActivity
                            if (enable && activity != null) {
                                if (!DeviceAuthHelper.canAuthenticateWithDevice(context)) {
                                    Toast.makeText(context, "Please set up fingerprint or screen lock in phone settings first", Toast.LENGTH_LONG).show()
                                    DeviceAuthHelper.openDeviceSecuritySettings(context)
                                } else {
                                    DeviceAuthHelper.promptDeviceAuthentication(
                                        activity = activity,
                                        title = "Enable App Lock",
                                        subtitle = "Verify with fingerprint or phone lock to enable",
                                        onSuccess = {
                                            preferencesRepository.setAppLockEnabled(true)
                                            Toast.makeText(context, "Device Lock protection enabled", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Verification failed: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else {
                                preferencesRepository.setAppLockEnabled(false)
                            }
                        }
                    )

                    SettingsToggleRow(
                        title = "Auto-Save Media to Gallery",
                        subtitle = "Automatically export received photos & videos to Gallery and files to Downloads",
                        checked = preferences.autoSaveToGallery,
                        onCheckedChange = { preferencesRepository.setAutoSaveToGallery(it) }
                    )

                    SettingsToggleRow(
                        title = "Require Confirmation",
                        subtitle = "Prompt before saving received files to local storage",
                        checked = preferences.requireConfirmationBeforeSave,
                        onCheckedChange = { preferencesRepository.setRequireConfirmationBeforeSave(it) }
                    )

                    SettingsToggleRow(
                        title = "Auto-Delete Temporary Chunks",
                        subtitle = "Purge intermediate frame cache after transfer completion",
                        checked = preferences.autoDeleteTempData,
                        onCheckedChange = { preferencesRepository.setAutoDeleteTempData(it) }
                    )
                }
            }
        }

        // Section 3: Appearance
        item {
            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Theme Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val modes = listOf("System", "Dark", "Light")
                    val selectedModeIdx = when (preferences.darkModePreference) {
                        "DARK" -> 1
                        "LIGHT" -> 2
                        else -> 0
                    }

                    GlassSegmentedControl(
                        items = modes,
                        selectedIndex = selectedModeIdx,
                        onItemSelected = { index ->
                            val mode = when (index) {
                                1 -> "DARK"
                                2 -> "LIGHT"
                                else -> "SYSTEM"
                            }
                            preferencesRepository.setDarkModePreference(mode)
                        }
                    )
                }
            }
        }

        // Section 4: Storage Management
        item {
            SettingsSectionHeader(title = "Local Storage", icon = Icons.Default.Storage)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Temporary Cache",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Used: $tempCacheSizeStr",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    GlassButton(
                        text = "Clear Cache",
                        icon = Icons.Default.CleaningServices,
                        isPrimary = false,
                        onClick = {
                            scope.launch {
                                clearTempCache(context)
                                tempCacheSizeStr = calculateTempCacheSize(context)
                                Toast.makeText(context, "Temporary files cleared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Section 5: Air-Gap Manifesto & About
        item {
            SettingsSectionHeader(title = "Air-Gap Manifesto", icon = Icons.Default.Info)
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DropQR • Version 1.0.0",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Protocol: DropQR Wire Protocol v1 (DQR1)\n" +
                                "• 100% Offline by Design\n" +
                                "• No telemetry or cloud analytics\n" +
                                "• Zero internet sockets used\n" +
                                "• AES-256-GCM + SHA-256 integrity verification",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppleBlue,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppleBlue,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

private fun calculateTempCacheSize(context: Context): String {
    val dir = File(context.filesDir, "transfers")
    if (!dir.exists()) return "0 B"
    val bytes = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private suspend fun clearTempCache(context: Context) = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "transfers")
    if (dir.exists()) {
        dir.deleteRecursively()
        dir.mkdirs()
    }
}
