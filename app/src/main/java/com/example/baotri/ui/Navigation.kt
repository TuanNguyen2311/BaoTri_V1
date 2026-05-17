package com.example.baotri.ui

sealed class Screen(val route: String) {
    // Auth
    object Login         : Screen("login")
    object ChangePassword : Screen("change_password/{userId}") {
        fun createRoute(userId: Long) = "change_password/$userId"
    }
    object SetupPin      : Screen("setup_pin/{userId}") {
        fun createRoute(userId: Long) = "setup_pin/$userId"
    }
    object PinReveal     : Screen("pin_reveal/{userId}") {
        fun createRoute(userId: Long) = "pin_reveal/$userId"
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
