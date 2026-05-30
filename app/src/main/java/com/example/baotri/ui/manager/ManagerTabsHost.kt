package com.example.baotri.ui.manager

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
import com.example.baotri.ui.manager.dashboard.ManagerBottomNav
import com.example.baotri.ui.manager.dashboard.ManagerDashboardScreen
import com.example.baotri.ui.manager.device.DeviceListScreen
import com.example.baotri.ui.manager.report.ReportScreen
import com.example.baotri.ui.manager.settings.SettingsScreen

@Composable
fun ManagerTabsHost(
    onNavigateToDeviceDetail: (Long) -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToEditDevice: (Long) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onLogout: () -> Unit,
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentTab = when (currentRoute) {
        Screen.ManagerDashboard.route -> 0
        Screen.DeviceList.route       -> 1
        Screen.Reports.route          -> 2
        Screen.ManagerSettings.route  -> 3
        else                          -> 0
    }

    Scaffold(
        bottomBar = {
            ManagerBottomNav(
                current     = currentTab,
                onDashboard = { innerNav.navigateTab(Screen.ManagerDashboard.route) },
                onDevices   = { innerNav.navigateTab(Screen.DeviceList.route) },
                onReports   = { innerNav.navigateTab(Screen.Reports.route) },
                onSettings  = { innerNav.navigateTab(Screen.ManagerSettings.route) }
            )
        }
    ) { padding ->
        NavHost(
            navController       = innerNav,
            startDestination    = Screen.ManagerDashboard.route,
            modifier            = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition     = { fadeIn(tween(200)) },
            exitTransition      = { fadeOut(tween(150)) },
            popEnterTransition  = { fadeIn(tween(200)) },
            popExitTransition   = { fadeOut(tween(150)) }
        ) {
            composable(Screen.ManagerDashboard.route) {
                ManagerDashboardScreen(
                    onNavigateToDeviceDetail = onNavigateToDeviceDetail
                )
            }
            composable(Screen.DeviceList.route) {
                DeviceListScreen(
                    onNavigateToAddDevice    = onNavigateToAddDevice,
                    onNavigateToEditDevice   = onNavigateToEditDevice,
                    onNavigateToDeviceDetail = onNavigateToDeviceDetail
                )
            }
            composable(Screen.Reports.route) {
                ReportScreen()
            }
            composable(Screen.ManagerSettings.route) {
                SettingsScreen(
                    isManager            = true,
                    onLogout             = onLogout,
                    onNavigateToBackup   = onNavigateToBackup,
                    onNavigateToAccounts = onNavigateToAccounts
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