package com.example.baotri.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.baotri.domain.model.Role
import com.example.baotri.ui.auth.login.LoginScreen
import com.example.baotri.ui.auth.setup.*
import com.example.baotri.ui.ktv.dashboard.KtvDashboardScreen
import com.example.baotri.ui.ktv.detail.DeviceDetailScreen
import com.example.baotri.ui.ktv.history.KtvHistoryScreen
import com.example.baotri.ui.ktv.log.WriteLogScreen
import com.example.baotri.ui.ktv.scan.ScanScreen
import com.example.baotri.ui.manager.account.AccountManagementScreen
import com.example.baotri.ui.manager.backup.BackupRestoreScreen
import com.example.baotri.ui.manager.dashboard.ManagerDashboardScreen
import com.example.baotri.ui.manager.device.AddEditDeviceScreen
import com.example.baotri.ui.manager.device.DeviceListScreen
import com.example.baotri.ui.manager.report.ReportScreen
import com.example.baotri.ui.manager.settings.SettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URLDecoder

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val dashboardRoutes = setOf(Screen.KtvDashboard.route, Screen.ManagerDashboard.route)

    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route,
        enterTransition  = {
            if (targetState.destination.route in dashboardRoutes) {
                // Login → Dashboard: fade + trượt lên nhẹ
                fadeIn(tween(420, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 8 }
            } else {
                // Màn hình con: trượt vào từ phải
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(300))
            }
        },
        exitTransition   = {
            if (initialState.destination.route == Screen.Login.route &&
                targetState.destination.route in dashboardRoutes) {
                // Login thoát: fade out nhanh
                fadeOut(tween(250))
            } else {
                // Màn hình con đẩy sang trái khi push
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } +
                fadeOut(tween(180))
            }
        },
        popEnterTransition  = {
            // Back: màn hình trước trượt vào từ trái
            slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } +
            fadeIn(tween(300))
        },
        popExitTransition   = {
            // Màn hình hiện tại trượt ra phải khi back
            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
            fadeOut(tween(200))
        }
    ) {

        // ── Login ─────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToChangePassword = { userId, role ->
                    navController.navigate(
                        Screen.ChangePassword.createRoute(userId, role.name)
                    ) { popUpTo(Screen.Login.route) { inclusive = true } }
                },
                onNavigateToKtvDashboard = {
                    navController.navigate(Screen.KtvDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToManagerDashboard = {
                    navController.navigate(Screen.ManagerDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = { username ->
                    navController.navigate(Screen.ForgotPassword.createRoute(username))
                }
            )
        }

        // ── ForgotPassword ────────────────────────────────────
        composable(
            route = Screen.ForgotPassword.route,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            ForgotPasswordScreen(
                onNavigateBack   = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── ChangePassword ────────────────────────────────────
        composable(
            route = Screen.ChangePassword.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("role")   { type = NavType.StringType }
            )
        ) { back ->
            val userId = back.arguments!!.getLong("userId")
            ChangePasswordScreen(
                userId = userId,
                // Không pop ChangePassword → SetupPin có thể back về đây
                onNavigateToSetupPin = { uid ->
                    navController.navigate(Screen.SetupPin.createRoute(uid))
                }
            )
        }

        // ── SetupPin ──────────────────────────────────────────
        composable(
            route = Screen.SetupPin.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { back ->
            val userId = back.arguments!!.getLong("userId")
            SetupPinScreen(
                userId          = userId,
                onNavigateBack  = { navController.popBackStack() },
                // PIN không truyền qua route — PinReveal đọc từ SetupPinViewModel
                onNavigateToPinReveal = { uid ->
                    // Không pop SetupPin khỏi back stack — PinReveal cần ViewModel của nó
                    navController.navigate(Screen.PinReveal.createRoute(uid))
                }
            )
        }

        // ── PinReveal — đọc PIN từ SetupPinViewModel ──────────
        // PIN KHÔNG truyền qua route để tránh lộ trong back stack
        composable(
            route = Screen.PinReveal.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { back ->
            // Lấy ViewModel của SetupPin (vẫn còn trong back stack)
            val setupPinEntry = remember(back) {
                runCatching { navController.getBackStackEntry(Screen.SetupPin.route) }.getOrNull()
            }
            val setupPinVm: SetupPinViewModel? = setupPinEntry?.let { hiltViewModel(it) }
            val fallbackFlow = remember { MutableStateFlow(SetupPinUiState()) }
            val pinState by (setupPinVm?.state ?: fallbackFlow).collectAsState()

            PinRevealScreen(
                pin = pinState.revealedPin,
                onNavigateToDashboard = {
                    navController.navigate(Screen.ManagerDashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── KTV ───────────────────────────────────────────────
        composable(Screen.KtvDashboard.route) {
            KtvDashboardScreen(
                onNavigateToScan     = { navController.navigate(Screen.ScanQr.route) },
                onNavigateToDetail   = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) },
                onNavigateToHistory  = { navController.navigate(Screen.KtvHistory.route) },
                onNavigateToSettings = { navController.navigate(Screen.KtvSettings.route) }
            )
        }

        composable(Screen.ScanQr.route) {
            ScanScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDevice = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
        ) {
            DeviceDetailScreen(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToWriteLog = { id -> navController.navigate(Screen.WriteLog.createRoute(id)) }
            )
        }

        composable(
            route = Screen.WriteLog.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.LongType },
                navArgument("logId")    { type = NavType.LongType; defaultValue = 0L }
            )
        ) {
            WriteLogScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.KtvHistory.route) {
            KtvHistoryScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDevice = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(Screen.KtvSettings.route) {
            SettingsScreen(
                isManager          = false,
                onLogout           = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                onNavigateToBackup = {},
                onNavigateToAccounts = {}
            )
        }

        // ── Manager ───────────────────────────────────────────
        composable(Screen.ManagerDashboard.route) {
            ManagerDashboardScreen(
                onNavigateToDevices      = { navController.navigate(Screen.DeviceList.route) },
                onNavigateToReports      = { navController.navigate(Screen.Reports.route) },
                onNavigateToSettings     = { navController.navigate(Screen.ManagerSettings.route) },
                onNavigateToDeviceDetail = { id -> navController.navigate(Screen.DeviceDetail.createRoute(id)) }
            )
        }

        composable(Screen.DeviceList.route) {
            DeviceListScreen(
                onNavigateBack           = { navController.popBackStack() },
                onNavigateToAddDevice    = { navController.navigate(Screen.AddEditDevice.createRoute(0L)) },
                onNavigateToEditDevice   = { id -> navController.navigate(Screen.AddEditDevice.createRoute(id)) },
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
                isManager            = true,
                onLogout             = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                onNavigateToBackup   = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToAccounts = { navController.navigate(Screen.AccountManagement.route) }
            )
        }
    }
}