package com.example.baotri.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,           // Unique device code e.g. TBM-001
    val name: String,
    val category: String,
    val location: String,
    val buyDate: Long? = null,
    val warrantyDate: Long? = null,
    val photoPath: String? = null,
    val qrPath: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
