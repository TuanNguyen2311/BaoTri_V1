package com.example.baotri.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.baotri.ui.auth.login.LoginScreen
import com.example.baotri.ui.auth.setup.*
import com.example.baotri.ui.ktv.dashboard.KtvDashboardScreen
import com.example.baotri.ui.ktv.detail.DeviceDetailScreen
import com.example.baotri.ui.ktv.log.WriteLogScreen
import com.example.baotri.ui.ktv.scan.ScanScreen
import com.example.baotri.ui.manager.account.AccountManagementScreen
import com.example.baotri.ui.manager.backup.BackupRestoreScreen
import com.example.baotri.ui.manager.dashboard.ManagerDashboardScreen
import com.example.baotri.ui.manager.device.AddEditDeviceScreen
import com.example.baotri.ui.manager.device.DeviceListScreen
import com.example.baotri.ui.manager.report.ReportScreen
import com.example.baotri.ui.manager.settings.SettingsScreen
import com.example.baotri.util.SessionManager
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        // ── Auth ─────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToChangePassword = { userId ->
                    navController.navigate(Screen.ChangePassword.createRoute(userId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToKtvDashboard = { userId ->
                    navController.navigate(Screen.KtvDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToManagerDashboard = { userId ->
                    navController.navigate(Screen.ManagerDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(
            route = Screen.ChangePassword.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { back ->
            val userId = back.arguments!!.getLong("userId")
            ChangePasswordScreen(
                userId = userId,
                onNavigateToSetupPin = { uid ->
                    navController.navigate(Screen.SetupPin.createRoute(uid)) {
                        popUpTo(Screen.ChangePassword.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SetupPin.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { back ->
            val userId = back.arguments!!.getLong("userId")
            SetupPinScreen(
                userId = userId,
                onNavigateToPinReveal = { uid, pin ->
                    navController.navigate(Screen.PinReveal.createRoute(uid) + "?pin=$pin") {
                        popUpTo(Screen.SetupPin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PinReveal.route + "?pin={pin}",
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("pin") { type = NavType.StringType; defaultValue = "" }
            )
        ) { back ->
            val pin = back.arguments?.getString("pin") ?: ""
            PinRevealScreen(
                pin = pin,
                onNavigateToDashboard = {
                    navController.navigate(Screen.ManagerDashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── KTV ──────────────────────────────────────────────
        composable(Screen.KtvDashboard.route) {
            KtvDashboardScreen(
                onNavigateToScan = { navController.navigate(Screen.ScanQr.route) },
                onNavigateToDetail = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) },
                onNavigateToHistory = { navController.navigate(Screen.KtvHistory.route) },
                onNavigateToSettings = { navController.navigate(Screen.KtvSettings.route) }
            )
        }

        composable(Screen.ScanQr.route) {
            ScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDevice = { id ->
                    navController.navigate(Screen.DeviceDetail.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
        ) { back ->
            val deviceId = back.arguments!!.getLong("deviceId")
            DeviceDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWriteLog = { id ->
                    navController.navigate(Screen.WriteLog.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.WriteLog.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.LongType },
                navArgument("logId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) {
            WriteLogScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.KtvHistory.route) {
            KtvHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDevice = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(Screen.KtvSettings.route) {
            SettingsScreen(
                isManager = false,
                onLogout = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToBackup = {},
                onNavigateToAccounts = {}
            )
        }

        // ── Manager ───────────────────────────────────────────
        composable(Screen.ManagerDashboard.route) {
            ManagerDashboardScreen(
                onNavigateToDevices = { navController.navigate(Screen.DeviceList.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToSettings = { navController.navigate(Screen.ManagerSettings.route) },
                onNavigateToDeviceDetail = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(Screen.DeviceList.route) {
            DeviceListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddDevice = { navController.navigate(Screen.AddEditDevice.createRoute(0L)) },
                onNavigateToEditDevice = { id -> navController.navigate(Screen.AddEditDevice.createRoute(id)) },
                onNavigateToDeviceDetail = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.AddEditDevice.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType; defaultValue = 0L })
        ) {
            AddEditDeviceScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Reports.route) {
            ReportScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AccountManagement.route) {
            AccountManagementScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ManagerSettings.route) {
            SettingsScreen(
                isManager = true,
                onLogout = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToAccounts = { navController.navigate(Screen.AccountManagement.route) }
            )
        }
    }
}

// Placeholder KtvHistoryScreen inline
@Composable
private fun KtvHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevice: (Long) -> Unit
) {
    // Full implementation below
    com.example.baotri.ui.ktv.history.KtvHistoryScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToDevice = onNavigateToDevice
    )
}
