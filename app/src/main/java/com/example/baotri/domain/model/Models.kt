package com.example.baotri.domain.model

// ── User ────────────────────────────────────────────────────
enum class Role { MANAGER, TECHNICIAN }

data class User(
    val id: Long,
    val username: String,
    val role: Role,
    val fullName: String,
    val isActive: Boolean,
    val isFirstLogin: Boolean
)

// ── Device ──────────────────────────────────────────────────
data class Device(
    val id: Long,
    val code: String,
    val name: String,
    val category: String,
    val location: String,
    val buyDate: Long?,
    val warrantyDate: Long?,
    val photoPath: String?,
    val qrPath: String?,
    val notes: String?,
    val latestStatus: MaintenanceStatus? = null,
    val logCount: Int = 0
) {
    fun isWarrantyExpired(now: Long): Boolean =
        warrantyDate != null && warrantyDate < now

    fun isWarrantyExpiringSoon(now: Long): Boolean {
        if (warrantyDate == null) return false
        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        return warrantyDate > now && warrantyDate < now + thirtyDays
    }
}

// ── Maintenance Log ─────────────────────────────────────────
enum class MaintenanceType {
    PERIODIC, EMERGENCY, INSPECTION;

    fun displayName() = when(this) {
        PERIODIC    -> "Bảo trì định kỳ"
        EMERGENCY   -> "Sửa chữa đột xuất"
        INSPECTION  -> "Kiểm tra"
    }
}

enum class MaintenanceStatus {
    RESOLVED, WAITING_PARTS, UNRESOLVED;

    fun displayName() = when(this) {
        RESOLVED       -> "Đã xử lý xong"
        WAITING_PARTS  -> "Chờ phụ tùng"
        UNRESOLVED     -> "Chưa xử lý"
    }
}

// ── Log Status History ──────────────────────────────────────
data class LogStatusHistory(
    val id: Long,
    val logId: Long,
    val status: MaintenanceStatus,
    val changedByUserId: Long?,
    val changedByName: String,
    val note: String,
    val photoPaths: List<String>,
    val changedAt: Long
)

data class MaintenanceLog(
    val id: Long,
    val deviceId: Long,
    val deviceName: String = "",
    val deviceCode: String = "",
    val userId: Long?,
    val performedByName: String,
    val logType: MaintenanceType,
    val description: String,
    val solution: String,
    val status: MaintenanceStatus,
    val photoPaths: List<String>,
    val notes: String?,
    val isDraft: Boolean,
    val performedAt: Long,
    val createdAt: Long,
    val statusHistory: List<LogStatusHistory> = emptyList()
) {
    val canUpdateStatus: Boolean
        get() = !isDraft && status != MaintenanceStatus.RESOLVED
}

// ── Dashboard Stats ─────────────────────────────────────────
data class ManagerStats(
    val totalDevices: Int,
    val pendingCount: Int,
    val expiredWarrantyCount: Int,
    val logsThisMonth: Int
)

data class KtvStats(
    val scannedToday: Int,
    val pendingLogs: Int
)

data class WeeklyLogCount(
    val week: Int,
    val current: Int,
    val previous: Int
)

data class DeviceIncident(
    val device: Device,
    val count: Int
)

// ── Backup ──────────────────────────────────────────────────
data class BackupHistory(
    val id: Long,
    val action: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val destination: String,
    val createdAt: Long
)
