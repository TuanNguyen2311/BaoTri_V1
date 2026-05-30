package com.example.baotri.ui

sealed class Screen(val route: String) {
    // ── Auth ──────────────────────────────────────────────────
    object Login : Screen("login")

    object ChangePassword : Screen("change_password/{userId}/{role}") {
        fun createRoute(userId: Long, role: String) = "change_password/$userId/$role"
    }

    object SetupPin : Screen("setup_pin/{userId}/{role}") {
        fun createRoute(userId: Long, role: String) = "setup_pin/$userId/$role"
    }

    object PinReveal : Screen("pin_reveal/{userId}/{role}") {
        fun createRoute(userId: Long, role: String) = "pin_reveal/$userId/$role"
    }

    // ← Thêm username vào route — encode để tránh ký tự đặc biệt
    object ForgotPassword : Screen("forgot_password/{username}") {
        fun createRoute(username: String): String {
            val encoded = java.net.URLEncoder.encode(username, "UTF-8")
            return "forgot_password/$encoded"
        }
    }

    // ── KTV ───────────────────────────────────────────────────
    object KtvTabs      : Screen("ktv_tabs")
    object KtvDashboard : Screen("ktv_dashboard")
    object ScanQr       : Screen("scan_qr")

    object DeviceDetail : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: Long) = "device_detail/$deviceId"
    }

    object WriteLog : Screen("write_log/{deviceId}?logId={logId}") {
        fun createRoute(deviceId: Long, logId: Long = 0L) =
            "write_log/$deviceId?logId=$logId"
    }

    object KtvHistory  : Screen("ktv_history")
    object KtvSettings : Screen("ktv_settings")

    // ── Manager ───────────────────────────────────────────────
    object ManagerTabs        : Screen("manager_tabs")
    object ManagerDashboard   : Screen("manager_dashboard")
    object DeviceList         : Screen("device_list")

    object AddEditDevice : Screen("add_edit_device/{deviceId}") {
        fun createRoute(deviceId: Long = 0L) = "add_edit_device/$deviceId"
    }

    object Reports            : Screen("reports")
    object AccountManagement  : Screen("account_management")
    object BackupRestore      : Screen("backup_restore")
    object ManagerSettings    : Screen("manager_settings")
}