package com.example.baotri.data.repository

import com.example.baotri.data.model.*
import com.example.baotri.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

// ── User ────────────────────────────────────────────────────
fun UserEntity.toDomain() = User(
    id           = id,
    username     = username,
    role         = if (role == UserRole.MANAGER) Role.MANAGER else Role.TECHNICIAN,
    fullName     = fullName,
    isActive     = isActive,
    isFirstLogin = isFirstLogin
)

// ── Device ──────────────────────────────────────────────────
fun DeviceEntity.toDomain(latestStatus: MaintenanceStatus? = null, logCount: Int = 0) = Device(
    id           = id,
    code         = code,
    name         = name,
    category     = category,
    location     = location,
    buyDate      = buyDate,
    warrantyDate = warrantyDate,
    photoPath    = photoPath,
    qrPath       = qrPath,
    notes        = notes,
    latestStatus = latestStatus,
    logCount     = logCount
)

fun Device.toEntity() = DeviceEntity(
    id           = id,
    code         = code,
    name         = name,
    category     = category,
    location     = location,
    buyDate      = buyDate,
    warrantyDate = warrantyDate,
    photoPath    = photoPath,
    qrPath       = qrPath,
    notes        = notes,
    updatedAt    = System.currentTimeMillis()
)

// ── LogStatusHistory ─────────────────────────────────────────
fun LogStatusHistoryEntity.toDomain() = LogStatusHistory(
    id              = id,
    logId           = logId,
    status          = when(status) {
        LogStatus.RESOLVED      -> MaintenanceStatus.RESOLVED
        LogStatus.WAITING_PARTS -> MaintenanceStatus.WAITING_PARTS
        LogStatus.UNRESOLVED    -> MaintenanceStatus.UNRESOLVED
    },
    changedByUserId = changedByUserId,
    changedByName   = changedByName,
    note            = note,
    photoPaths      = if (photoPaths.isBlank()) emptyList()
                      else gson.fromJson(photoPaths, object : TypeToken<List<String>>() {}.type),
    changedAt       = changedAt
)

fun LogStatusHistory.toEntity() = LogStatusHistoryEntity(
    id              = id,
    logId           = logId,
    status          = when(status) {
        MaintenanceStatus.RESOLVED      -> LogStatus.RESOLVED
        MaintenanceStatus.WAITING_PARTS -> LogStatus.WAITING_PARTS
        MaintenanceStatus.UNRESOLVED    -> LogStatus.UNRESOLVED
    },
    changedByUserId = changedByUserId,
    changedByName   = changedByName,
    note            = note,
    photoPaths      = gson.toJson(photoPaths),
    changedAt       = changedAt
)

// ── MaintenanceLog ───────────────────────────────────────────
fun MaintenanceLogEntity.toDomain(
    deviceName: String = "",
    deviceCode: String = "",
    statusHistory: List<LogStatusHistory> = emptyList()
) = MaintenanceLog(
    id              = id,
    deviceId        = deviceId,
    deviceName      = deviceName,
    deviceCode      = deviceCode,
    userId          = userId,
    performedByName = performedByName,
    logType         = when(logType) {
        LogType.PERIODIC    -> MaintenanceType.PERIODIC
        LogType.EMERGENCY   -> MaintenanceType.EMERGENCY
        LogType.INSPECTION  -> MaintenanceType.INSPECTION
    },
    description     = description,
    solution        = solution,
    status          = when(status) {
        LogStatus.RESOLVED      -> MaintenanceStatus.RESOLVED
        LogStatus.WAITING_PARTS -> MaintenanceStatus.WAITING_PARTS
        LogStatus.UNRESOLVED    -> MaintenanceStatus.UNRESOLVED
    },
    photoPaths      = if (photoPaths.isBlank()) emptyList()
                      else gson.fromJson(photoPaths, object : TypeToken<List<String>>() {}.type),
    notes           = notes,
    isDraft         = isDraft,
    performedAt     = performedAt,
    createdAt       = createdAt,
    statusHistory   = statusHistory
)

fun MaintenanceLog.toEntity() = MaintenanceLogEntity(
    id              = id,
    deviceId        = deviceId,
    userId          = userId,
    performedByName = performedByName,
    logType         = when(logType) {
        MaintenanceType.PERIODIC    -> LogType.PERIODIC
        MaintenanceType.EMERGENCY   -> LogType.EMERGENCY
        MaintenanceType.INSPECTION  -> LogType.INSPECTION
    },
    description     = description,
    solution        = solution,
    status          = when(status) {
        MaintenanceStatus.RESOLVED      -> LogStatus.RESOLVED
        MaintenanceStatus.WAITING_PARTS -> LogStatus.WAITING_PARTS
        MaintenanceStatus.UNRESOLVED    -> LogStatus.UNRESOLVED
    },
    photoPaths      = gson.toJson(photoPaths),
    notes           = notes,
    isDraft         = isDraft,
    performedAt     = performedAt,
    createdAt       = createdAt
)
