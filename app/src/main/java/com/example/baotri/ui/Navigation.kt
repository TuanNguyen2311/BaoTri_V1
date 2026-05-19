package com.example.baotri.ui

import com.example.baotri.domain.model.Role

sealed class Screen(val route: String) {
    // Auth
    object Login         : Screen("login")
    object ChangePassword : Screen("change_password/{userId}/{role}") {
        fun createRoute(userId: Long, role: Role) = "change_password/$userId/$role"
    }
    object SetupPin      : Screen("setup_pin/{userId}/{role}") {
        fun createRoute(userId: Long, role: Role) = "setup_pin/$userId/$role"
    }
    object PinReveal     : Screen("pin_reveal/{userId}/{role}") {
        fun createRoute(userId: Long, role: Role) = "pin_reveal/$userId/$role"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword  : Screen("reset_password/{userId}") {
        fun createRoute(userId: Long) = "reset_password/$userId"
    }

    // KTV
    object KtvDashboard  : Screen("ktv_dashboard")
    object ScanQr        : Screen("scan_qr")
    object DeviceDetail  : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: Long) = "device_detail/$deviceId"
    }
    object WriteLog      : Screen("write_log/{deviceId}?logId={logId}") {
        fun createRoute(deviceId: Long, logId: Long = 0L) = "write_log/$deviceId?logId=$logId"
    }
    object KtvHistory    : Screen("ktv_history")
    object KtvSettings   : Screen("ktv_settings")

    // Manager
    object ManagerDashboard : Screen("manager_dashboard")
    object DeviceList       : Screen("device_list")
    object AddEditDevice    : Screen("add_edit_device/{deviceId}") {
        fun createRoute(deviceId: Long = 0L) = "add_edit_device/$deviceId"
    }
    object Reports          : Screen("reports")
    object AccountManagement : Screen("account_management")
    object BackupRestore    : Screen("backup_restore")
    object ManagerSettings  : Screen("manager_settings")
}
