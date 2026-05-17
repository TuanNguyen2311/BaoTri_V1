package com.example.baotri.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LogType {
    PERIODIC,       // Bảo trì định kỳ
    EMERGENCY,      // Sửa chữa đột xuất
    INSPECTION      // Kiểm tra
}

enum class LogStatus {
    RESOLVED,       // Đã xử lý xong
    WAITING_PARTS,  // Chờ phụ tùng
    UNRESOLVED      // Chưa xử lý
}

@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("deviceId"), Index("userId")]
)
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val userId: Long?,
    val performedByName: String,    // Snapshot of name at log time
    val logType: LogType,
    val description: String,        // Mô tả sự cố
    val solution: String,           // Cách xử lý
    val status: LogStatus,
    val photoPaths: String = "",    // JSON array of photo paths
    val notes: String? = null,
    val isDraft: Boolean = false,
    val performedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
