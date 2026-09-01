package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserPreferencesRepository
import com.example.ui.components.AirGapBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleSecurity
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    preferencesRepository: UserPreferencesRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "Transfer Without Internet",
            description = "Share data directly between screens using high-density animated QR streaming. 100% offline and air-gapped.",
            icon = Icons.Default.WifiOff,
            accentColor = EmeraldGreen
        ),
        OnboardingStep(
            title = "Just Scan & Transfer",
            description = "Point the camera at any sender's screen. DropQR captures frames in parallel and auto-reassembles complete files.",
            icon = Icons.Default.QrCodeScanner,
            accentColor = ElectricCyan
        ),
        OnboardingStep(
            title = "End-to-End Encrypted",
            description = "All payloads are protected with AES-256-GCM encryption and verified with SHA-256 checksums locally.",
            icon = Icons.Default.Lock,
            accentColor = PurpleSecurity
        ),
        OnboardingStep(
            title = "Send Anything",
            description = "Transmit photos, documents, contacts, encrypted notes, and web links instantly with zero cloud dependency.",
            icon = Icons.Default.FolderShared,
            accentColor = AppleBlue
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Air-Gap Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AirGapBadge()
        }

        // Slide Content Animated Container
        AnimatedContent(
            targetState = steps[currentStep],
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_step"
        ) { step ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                elevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(step.accentColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = step.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = step.description,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Bottom Controls: Page Dots & Next/Get Started Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, _ ->
                    val isSelected = index == currentStep
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) AppleBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep < steps.size - 1) {
                    GlassButton(
                        text = "Skip",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                preferencesRepository.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    )

                    GlassButton(
                        text = "Continue",
                        icon = Icons.Default.ArrowForward,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { currentStep++ }
                    )
                } else {
                    GlassButton(
                        text = "Get Started",
                        isPrimary = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_get_started"),
                        onClick = {
                            scope.launch {
                                preferencesRepository.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}
