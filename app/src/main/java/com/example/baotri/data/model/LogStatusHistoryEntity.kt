package com.example.baotri.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_status_history",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["changedByUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("logId"), Index("changedByUserId")]
)
data class LogStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    val status: LogStatus,
    val changedByUserId: Long?,
    val changedByName: String,      // Snapshot tên tại thời điểm đó
    val note: String = "",          // Ghi chú cho bước này
    val photoPaths: String = "",    // JSON array ảnh đính kèm riêng cho bước này
    val changedAt: Long = System.currentTimeMillis()
)
