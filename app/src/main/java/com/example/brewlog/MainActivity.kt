package com.example.brewlog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.*
import com.example.brewlog.ui.AddBrewScreen
import com.example.brewlog.ui.components.CoffeeBeanIcon
import com.example.brewlog.ui.BrewViewModel
import com.example.brewlog.ui.DialInScreen
import com.example.brewlog.ui.EditMachineScreen
import com.example.brewlog.ui.EspressoMachineScreen
import com.example.brewlog.ui.HomeScreen
import com.example.brewlog.ui.SettingsScreen
import com.example.brewlog.ui.SettingsViewModel
import com.example.brewlog.ui.ShotHistoryScreen
import com.example.brewlog.ui.theme.BrewLogTheme
import com.example.brewlog.util.NotificationHelper
import com.example.brewlog.worker.MaintenanceReminderWorker
import java.util.concurrent.TimeUnit

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
        scheduleMaintenanceCheck()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            BrewLogTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val viewModel: BrewViewModel = viewModel()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentDestination == "home" || currentDestination == "espresso_machine" || currentDestination == "dial_in" || currentDestination == "shot_history") {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentDestination == "home",
                                    onClick = {
                                        if (currentDestination != "home") {
                                            navController.navigate("home") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { CoffeeBeanIcon(modifier = Modifier.size(22.dp)) },
                                    label = { Text(stringResource(R.string.my_brews), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
                                )
                                NavigationBarItem(
                                    selected = currentDestination == "dial_in",
                                    onClick = {
                                        if (currentDestination != "dial_in") {
                                            navController.navigate("dial_in") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                    label = { Text("Dial-In", maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
                                )
                                NavigationBarItem(
                                    selected = currentDestination == "shot_history",
                                    onClick = {
                                        if (currentDestination != "shot_history") {
                                            navController.navigate("shot_history") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text(stringResource(R.string.shot_history), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
                                )
                                NavigationBarItem(
                                    selected = currentDestination == "espresso_machine",
                                    onClick = {
                                        if (currentDestination != "espresso_machine") {
                                            navController.navigate("espresso_machine") {
                                                popUpTo("home") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.CoffeeMaker, contentDescription = null) },
                                    label = { Text(stringResource(R.string.espresso_machine), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                        enterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally { fullWidth -> fullWidth / 4 } },
                        exitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally { fullWidth -> -fullWidth / 4 } },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally { fullWidth -> -fullWidth / 4 } },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally { fullWidth -> fullWidth / 4 } }
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAddBrew = { navController.navigate("add_brew") },
                                onNavigateToEditBrew = { logId -> navController.navigate("edit_brew/$logId") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("espresso_machine") {
                            EspressoMachineScreen(
                                viewModel = viewModel,
                                onNavigateToEdit = { navController.navigate("edit_machine") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("edit_machine") {
                            EditMachineScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = settingsViewModel
                            )
                        }
                        composable("dial_in") {
                            DialInScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSettings = { navController.navigate("settings") }
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
                                onNavigateToSettings = { navController.navigate("settings") },
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
                                    navController.navigate("dial_in/$beanId/$basketType/${dose.toFloat()}/${grind}")
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("add_brew") {
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
                            arguments = listOf(navArgument("logId") { type = NavType.IntType })
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
                }
            }
        }
    }
}
