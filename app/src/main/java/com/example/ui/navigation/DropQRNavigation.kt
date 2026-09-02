package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.DropQRApplication
import com.example.protocol.TransferPayloadType
import com.example.ui.components.AmbientBackground
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReceiverScreen
import com.example.ui.screens.SenderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SteganographyScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.BentoActivePill
import com.example.ui.theme.BentoActivePillDark
import com.example.ui.theme.BentoPrimaryBlue
import com.example.ui.theme.BentoPrimaryBlueDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.BentoSurfaceLight
import com.example.ui.theme.BentoTextSecondaryDark
import com.example.ui.theme.BentoTextSecondaryLight
import com.example.ui.theme.LocalIsDark

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Send : Screen("send?type={type}", "Send", Icons.Default.Send) {
        fun createRoute(type: TransferPayloadType? = null) = if (type != null) "send?type=${type.name}" else "send"
    }
    object Receive : Screen("receive", "Receive", Icons.Default.QrCodeScanner)
    object Steganography : Screen("steganography", "Stego Ghost Mode")
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun DropQRAppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as DropQRApplication
    val transferRepository = app.transferRepository
    val preferencesRepository = app.preferencesRepository

    val preferences by preferencesRepository.preferencesFlow.collectAsState()
    var isUnlocked by remember { mutableStateOf(!preferences.appLockEnabled) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination = if (preferences.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    val showBottomNav = currentRoute in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Settings.route
    )

    AmbientBackground {
        if (preferences.appLockEnabled && !isUnlocked) {
            AppLockScreen(
                onUnlocked = { isUnlocked = true }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() },
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        preferencesRepository = preferencesRepository,
                        onComplete = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToSend = { type ->
                            navController.navigate(Screen.Send.createRoute(type))
                        },
                        onNavigateToReceive = {
                            navController.navigate(Screen.Receive.route)
                        },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        modifier = if (showBottomNav) Modifier.padding(bottom = 84.dp) else Modifier
                    )
                }

                composable(
                    route = Screen.Send.route,
                    arguments = listOf(navArgument("type") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val typeParam = backStackEntry.arguments?.getString("type")
                    val initialType = typeParam?.let {
                        try {
                            TransferPayloadType.valueOf(it)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    SenderScreen(
                        initialType = initialType,
                        transferRepository = transferRepository,
                        preferencesRepository = preferencesRepository,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Receive.route) {
                    ReceiverScreen(
                        transferRepository = transferRepository,
                        preferencesRepository = preferencesRepository,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        transferRepository = transferRepository,
                        onBack = { navController.popBackStack() },
                        modifier = if (showBottomNav) Modifier.padding(bottom = 84.dp) else Modifier
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        preferencesRepository = preferencesRepository,
                        onBack = { navController.popBackStack() },
                        modifier = if (showBottomNav) Modifier.padding(bottom = 84.dp) else Modifier
                    )
                }
            }

            // Clean Bento Floating Navigation Bar
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp, start = 24.dp, end = 24.dp)
            ) {
                FloatingBentoNavBar(
                    currentRoute = currentRoute,
                    onNavigateHome = {
                        if (currentRoute != Screen.Home.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateSend = {
                        navController.navigate(Screen.Send.createRoute(null))
                    },
                    onNavigateReceive = {
                        navController.navigate(Screen.Receive.route)
                    },
                    onNavigateHistory = {
                        if (currentRoute != Screen.History.route) {
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNavigateSettings = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
private fun FloatingBentoNavBar(
    currentRoute: String?,
    onNavigateHome: () -> Unit,
    onNavigateSend: () -> Unit,
    onNavigateReceive: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val isDark = LocalIsDark.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.08f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color(0xFF0F172A).copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) BentoSurfaceDark else BentoSurfaceLight)
            .border(
                BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            BentoNavBarItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentRoute == Screen.Home.route,
                onClick = onNavigateHome
            )

            // Send
            BentoNavBarItem(
                icon = Icons.Default.Send,
                label = "Send",
                isSelected = currentRoute?.startsWith("send") == true,
                onClick = onNavigateSend
            )

            // Receive
            BentoNavBarItem(
                icon = Icons.Default.QrCodeScanner,
                label = "Receive",
                isSelected = currentRoute == Screen.Receive.route,
                onClick = onNavigateReceive
            )

            // History
            BentoNavBarItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = currentRoute == Screen.History.route,
                onClick = onNavigateHistory
            )

            // Settings
            BentoNavBarItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentRoute == Screen.Settings.route,
                onClick = onNavigateSettings
            )
        }
    }
}

@Composable
private fun BentoNavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    val activePillBg = if (isDark) BentoActivePillDark else BentoActivePill
    val activeColor = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
    val inactiveColor = if (isDark) BentoTextSecondaryDark else BentoTextSecondaryLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .padding(horizontal = 2.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(activePillBg)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    } else {
                        Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}
