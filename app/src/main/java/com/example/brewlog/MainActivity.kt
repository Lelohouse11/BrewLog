package com.example.brewlog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.brewlog.ui.AddBrewScreen
import com.example.brewlog.ui.BrewViewModel
import com.example.brewlog.ui.DialInScreen
import com.example.brewlog.ui.EditMachineScreen
import com.example.brewlog.ui.EspressoMachineScreen
import com.example.brewlog.ui.HomeScreen
import com.example.brewlog.ui.SettingsScreen
import com.example.brewlog.ui.SettingsViewModel
import com.example.brewlog.ui.ShotHistoryScreen
import com.example.brewlog.ui.components.FloatingGlassNavigationBar
import com.example.brewlog.ui.components.cremaGlow
import com.example.brewlog.ui.theme.*
import com.example.brewlog.util.NotificationHelper
import com.example.brewlog.worker.MaintenanceReminderWorker
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : AppCompatActivity() {
    private fun scheduleMaintenanceCheck() {
        val workRequest = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(24, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MaintenanceCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
        scheduleMaintenanceCheck()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            var showSplashScreen by remember { mutableStateOf(true) }

            BrewLogTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val viewModel: BrewViewModel = viewModel()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination?.route

                // Solid background container prevents any gray flashes during transitions
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = {
                                fadeIn(animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.88f, animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                                slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 16 }, animationSpec = tween(480, easing = FastOutSlowInEasing))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(340, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.90f, animationSpec = tween(340, easing = FastOutSlowInEasing))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.88f, animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                                slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 16 }, animationSpec = tween(480, easing = FastOutSlowInEasing))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(340, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.90f, animationSpec = tween(340, easing = FastOutSlowInEasing))
                            }
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddBrew = { navController.navigate("add_brew") },
                                    onNavigateToEditBrew = { logId -> navController.navigate("edit_brew/$logId") }
                                )
                            }
                            composable("espresso_machine") {
                                EspressoMachineScreen(
                                    viewModel = viewModel,
                                    onNavigateToEdit = { navController.navigate("edit_machine") }
                                )
                            }
                            composable(
                                route = "edit_machine",
                                enterTransition = {
                                    slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing))
                                },
                                exitTransition = {
                                    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                }
                            ) {
                                EditMachineScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                            composable("dial_in") {
                                DialInScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onShotLogged = {
                                        navController.navigate("shot_history") {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            composable(
                                route = "dial_in/{beanId}/{basketType}/{dose}/{grind}",
                                arguments = listOf(
                                    navArgument("beanId") { type = NavType.IntType },
                                    navArgument("basketType") { type = NavType.StringType },
                                    navArgument("dose") { type = NavType.FloatType },
                                    navArgument("grind") { type = NavType.FloatType }
                                )
                            ) { backStackEntry ->
                                DialInScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onShotLogged = {
                                        navController.navigate("shot_history") {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    initialBeanId = backStackEntry.arguments?.getInt("beanId"),
                                    initialBasketType = backStackEntry.arguments?.getString("basketType"),
                                    initialDose = backStackEntry.arguments?.getFloat("dose")?.toDouble(),
                                    initialGrindSize = backStackEntry.arguments?.getFloat("grind")
                                )
                            }
                            composable("shot_history") {
                                ShotHistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateToDialIn = { beanId, basketType, dose, grind ->
                                        navController.navigate("dial_in/$beanId/$basketType/${dose.toFloat()}/$grind")
                                    }
                                )
                            }
                            composable(
                                route = "add_brew",
                                enterTransition = {
                                    slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing))
                                },
                                exitTransition = {
                                    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                }
                            ) {
                                AddBrewScreen(
                                    onSave = {
                                        viewModel.addLog(it)
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "edit_brew/{logId}",
                                arguments = listOf(navArgument("logId") { type = NavType.IntType }),
                                enterTransition = {
                                    slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing))
                                },
                                exitTransition = {
                                    slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 6 }, animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                }
                            ) { backStackEntry ->
                                val logId = backStackEntry.arguments?.getInt("logId") ?: return@composable
                                val logs by viewModel.allLogs.collectAsState()
                                val logToEdit = logs.find { it.id == logId }

                                AddBrewScreen(
                                    onSave = {
                                        viewModel.updateLog(it)
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = { navController.popBackStack() },
                                    existingLog = logToEdit
                                )
                            }
                        }

                        if (!showSplashScreen && (currentDestination == "home" || currentDestination == "espresso_machine" || currentDestination == "dial_in" || currentDestination == "shot_history" || currentDestination == "settings")) {
                            FloatingGlassNavigationBar(
                                currentDestination = currentDestination,
                                onNavigate = { destination ->
                                    if (currentDestination != destination) {
                                        navController.navigate(destination) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                onNavigateToSettings = {
                                    if (currentDestination != "settings") {
                                        navController.navigate("settings") {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                            )
                        }

                        // Crema Splash & Loading Transition Overlay
                        AnimatedVisibility(
                            visible = showSplashScreen,
                            exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing))
                        ) {
                            CremaSplashScreen(
                                onSplashFinished = { showSplashScreen = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CremaSplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splashScale"
    )

    val isDark = isAppInDarkTheme()
    val splashBg = if (isDark) CoffeeDarkBackground else CoffeeLightBackground

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200.milliseconds)
        onSplashFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = splashBg
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
            ) {
                // Golden Crema Glass Emblem Container
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (isDark) EspressoGlassBg else VellumGlassBg,
                    border = BorderStroke(1.5.dp, if (isDark) CremaAmber.copy(0.45f) else CremaAmberDark.copy(0.45f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(120.dp)
                        .cremaGlow(color = if (isDark) CremaAmber else CremaAmberDark, borderRadius = 32.dp, glowRadius = 12.dp, alpha = 0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_logo),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Title in Serif Font
                Text(
                    text = "BrewLog",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline in Monospace
                Text(
                    text = "CRAFT & PRECISION",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = BaristaMonospaceFontFamily,
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
