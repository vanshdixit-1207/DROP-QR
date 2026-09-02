package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.AirGapBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import com.example.util.DeviceAuthHelper

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val activity = context as? FragmentActivity

    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val isDeviceSecure = remember { DeviceAuthHelper.canAuthenticateWithDevice(context) }

    fun requestAuth() {
        if (activity == null) {
            onUnlocked()
            return
        }
        isAuthenticating = true
        authErrorMessage = null
        DeviceAuthHelper.promptDeviceAuthentication(
            activity = activity,
            title = "Unlock DropQR",
            subtitle = "Verify with device fingerprint, face, or phone PIN/Pattern",
            onSuccess = {
                isAuthenticating = false
                authErrorMessage = null
                onUnlocked()
            },
            onError = { error ->
                isAuthenticating = false
                authErrorMessage = error
            }
        )
    }

    // Trigger system authentication automatically when screen opens
    LaunchedEffect(Unit) {
        requestAuth()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Icon & Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(PurpleSecurity.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(2.dp, PurpleSecurity, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Device Authentication",
                    tint = PurpleSecurity,
                    modifier = Modifier.size(50.dp)
                )
            }

            Text(
                text = "DropQR Protected",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Authenticated with device lock (Fingerprint, Face, PIN or Pattern)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            AirGapBadge()
        }

        // Center Status / Instructions
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = if (isDeviceSecure) {
                        "Touch your fingerprint sensor or verify your phone screen lock."
                    } else {
                        "No screen lock or biometric is configured on this device."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                AnimatedVisibility(visible = authErrorMessage != null) {
                    Text(
                        text = authErrorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassButton(
                text = "Unlock with Device Lock",
                icon = Icons.Default.Fingerprint,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    requestAuth()
                }
            )

            if (!isDeviceSecure) {
                GlassButton(
                    text = "Open Phone Security Settings",
                    icon = Icons.Default.Settings,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        DeviceAuthHelper.openDeviceSecuritySettings(context)
                    }
                )

                GlassButton(
                    text = "Bypass (No Lock Set)",
                    icon = Icons.Default.LockOpen,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onUnlocked
                )
            }
        }
    }
}
