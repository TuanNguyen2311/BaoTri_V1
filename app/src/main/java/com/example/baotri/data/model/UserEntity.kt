package com.example.baotri.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { MANAGER, TECHNICIAN }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val pinHash: String? = null,        // Only manager has PIN
    val role: UserRole,
    val fullName: String,
    val isActive: Boolean = true,
    val isFirstLogin: Boolean = true,   // Force password change on first login
    val createdAt: Long = System.currentTimeMillis()
)
