package com.example.baotri.ui.ktv

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.baotri.ui.Screen
import com.example.baotri.ui.ktv.dashboard.KtvBottomNav
import com.example.baotri.ui.ktv.dashboard.KtvDashboardScreen
import com.example.baotri.ui.ktv.history.KtvHistoryScreen
import com.example.baotri.ui.ktv.scan.ScanScreen
import com.example.baotri.ui.manager.settings.SettingsScreen

@Composable
fun KtvTabsHost(
    onNavigateToDeviceDetail: (Long) -> Unit,
    onLogout: () -> Unit,
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentTab = when (currentRoute) {
        Screen.KtvDashboard.route -> 0
        Screen.ScanQr.route       -> 1
        Screen.KtvHistory.route   -> 2
        Screen.KtvSettings.route  -> 3
        else                      -> 0
    }

    Scaffold(
        bottomBar = {
            KtvBottomNav(
                current    = currentTab,
                onHome     = { innerNav.navigateTab(Screen.KtvDashboard.route) },
                onScan     = { innerNav.navigateTab(Screen.ScanQr.route) },
                onHistory  = { innerNav.navigateTab(Screen.KtvHistory.route) },
                onSettings = { innerNav.navigateTab(Screen.KtvSettings.route) }
            )
        }
    ) { padding ->
        NavHost(
            navController       = innerNav,
            startDestination    = Screen.KtvDashboard.route,
            modifier            = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition     = { fadeIn(tween(200)) },
            exitTransition      = { fadeOut(tween(150)) },
            popEnterTransition  = { fadeIn(tween(200)) },
            popExitTransition   = { fadeOut(tween(150)) }
        ) {
            composable(Screen.KtvDashboard.route) {
                KtvDashboardScreen(
                    onNavigateToScan     = { innerNav.navigateTab(Screen.ScanQr.route) },
                    onNavigateToHistory  = { innerNav.navigateTab(Screen.KtvHistory.route) },
                    onNavigateToDetail   = onNavigateToDeviceDetail
                )
            }
            composable(Screen.ScanQr.route) {
                ScanScreen(
                    onNavigateBack     = {},
                    onNavigateToDevice = onNavigateToDeviceDetail
                )
            }
            composable(Screen.KtvHistory.route) {
                KtvHistoryScreen(
                    onNavigateToDevice = onNavigateToDeviceDetail
                )
            }
            composable(Screen.KtvSettings.route) {
                SettingsScreen(
                    isManager            = false,
                    onLogout             = onLogout,
                    onNavigateToBackup   = {},
                    onNavigateToAccounts = {}
                )
            }
        }
    }
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}