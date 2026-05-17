package com.example.baotri.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BackupAction { EXPORT, IMPORT }

@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: BackupAction,
    val fileName: String,
    val fileSizeBytes: Long,
    val destination: String,    // "local", "share"
    val createdAt: Long = System.currentTimeMillis()
)
