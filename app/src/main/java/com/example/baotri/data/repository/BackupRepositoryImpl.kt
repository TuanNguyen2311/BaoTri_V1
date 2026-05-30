package com.example.baotri.data.repository

import com.example.baotri.data.db.AppDatabase
import com.example.baotri.data.db.dao.BackupHistoryDao
import com.example.baotri.data.model.*
import com.example.baotri.domain.model.BackupHistory
import com.example.baotri.domain.repository.BackupRepository
import com.example.baotri.util.SecurityUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val historyDao: BackupHistoryDao,
    private val gson: Gson
) : BackupRepository {

    override suspend fun exportBackup(): ByteArray {
        val users   = db.userDao().getAllForBackup()
        val devices = db.deviceDao().getAllForBackup()
        val logs    = db.maintenanceLogDao().getAll()
        val history = db.logStatusHistoryDao().getAll()

        val payload = mapOf(
            "version"    to "1.0.0",
            "createdAt"  to System.currentTimeMillis(),
            "users"      to users,
            "devices"    to devices,
            "logs"       to logs,
            "logHistory" to history
        )

        val jsonBytes = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val checksum  = SecurityUtil.checksum(jsonBytes)
        val withChecksum = "$checksum|${String(jsonBytes, Charsets.UTF_8)}".toByteArray(Charsets.UTF_8)
        return SecurityUtil.encrypt(withChecksum)
    }

    override suspend fun importBackup(data: ByteArray) {
        val decrypted = SecurityUtil.decrypt(data)
        val raw = String(decrypted, Charsets.UTF_8)

        val separatorIdx = raw.indexOf('|')
        require(separatorIdx > 0) { "Invalid backup format" }

        val storedChecksum = raw.substring(0, separatorIdx)
        val json           = raw.substring(separatorIdx + 1)

        val actualChecksum = SecurityUtil.checksum(json.toByteArray(Charsets.UTF_8))
        require(storedChecksum == actualChecksum) { "Backup file has been tampered with" }

        db.clearAllTables()
        restoreFromJson(json)
    }

    private suspend fun restoreFromJson(json: String) {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val root: Map<String, Any> = gson.fromJson(json, mapType)

        val usersJson      = gson.toJson(root["users"])
        val devicesJson    = gson.toJson(root["devices"])
        val logsJson       = gson.toJson(root["logs"])
        val historyJson    = gson.toJson(root["logHistory"])

        val userType    = object : TypeToken<List<UserEntity>>() {}.type
        val deviceType  = object : TypeToken<List<DeviceEntity>>() {}.type
        val logType     = object : TypeToken<List<MaintenanceLogEntity>>() {}.type
        val historyType = object : TypeToken<List<LogStatusHistoryEntity>>() {}.type

        val users:   List<UserEntity>?               = gson.fromJson(usersJson, userType)
        val devices: List<DeviceEntity>?             = gson.fromJson(devicesJson, deviceType)
        val logs:    List<MaintenanceLogEntity>?     = gson.fromJson(logsJson, logType)
        val history: List<LogStatusHistoryEntity>?  = gson.fromJson(historyJson, historyType)

        // Insert in FK-dependency order: users → devices → logs → history
        users?.forEach   { db.userDao().insert(it) }
        devices?.forEach { db.deviceDao().insert(it) }
        logs?.forEach    { db.maintenanceLogDao().insert(it) }
        history?.forEach { db.logStatusHistoryDao().insert(it) }
    }

    override fun getBackupHistory(): Flow<List<BackupHistory>> =
        historyDao.getRecent().map { list -> list.map { it.toDomain() } }

    override suspend fun getLatestBackup(): BackupHistory? =
        historyDao.getLatest()?.toDomain()

    override suspend fun recordBackup(action: String, fileName: String, sizeBytes: Long, destination: String) {
        historyDao.insert(
            BackupHistoryEntity(
                action        = BackupAction.valueOf(action),
                fileName      = fileName,
                fileSizeBytes = sizeBytes,
                destination   = destination
            )
        )
        historyDao.pruneOld()
    }

    private fun BackupHistoryEntity.toDomain() = BackupHistory(
        id            = id,
        action        = action.name,
        fileName      = fileName,
        fileSizeBytes = fileSizeBytes,
        destination   = destination,
        createdAt     = createdAt
    )
}
