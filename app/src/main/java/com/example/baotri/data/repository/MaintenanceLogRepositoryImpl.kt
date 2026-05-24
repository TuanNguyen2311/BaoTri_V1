package com.example.baotri.data.repository

import com.example.baotri.data.db.dao.DeviceDao
import com.example.baotri.data.db.dao.MaintenanceLogDao
import com.example.baotri.data.db.dao.LogStatusHistoryDao
import com.example.baotri.data.model.LogStatus
import com.example.baotri.data.model.LogStatusHistoryEntity
import com.example.baotri.domain.model.*
import com.example.baotri.domain.repository.MaintenanceLogRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

private val gson = Gson()

class MaintenanceLogRepositoryImpl @Inject constructor(
    private val logDao: MaintenanceLogDao,
    private val deviceDao: DeviceDao,
    private val historyDao: LogStatusHistoryDao
) : MaintenanceLogRepository {

    override fun getLogsForDevice(deviceId: Long): Flow<List<MaintenanceLog>> =
        logDao.getLogsForDevice(deviceId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getLogsByUser(userId: Long): Flow<List<MaintenanceLog>> =
        logDao.getLogsByUser(userId).map { list ->
            list.map { entity ->
                val device = deviceDao.findById(entity.deviceId)
                entity.toDomain(device?.name ?: "", device?.code ?: "")
            }
        }

    override fun getDraftsByUser(userId: Long): Flow<List<MaintenanceLog>> =
        logDao.getDraftsByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLogById(id: Long): MaintenanceLog? =
        logDao.findById(id)?.toDomain()

    override suspend fun getLogWithHistory(logId: Long): MaintenanceLog? {
        val entity = logDao.findById(logId) ?: return null
        val device = deviceDao.findById(entity.deviceId)
        val history = historyDao.getHistoryForLogOnce(logId).map { it.toDomain() }
        return entity.toDomain(
            deviceName    = device?.name ?: "",
            deviceCode    = device?.code ?: "",
            statusHistory = history
        )
    }

    override fun getStatusHistory(logId: Long): Flow<List<LogStatusHistory>> =
        historyDao.getHistoryForLog(logId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveLog(log: MaintenanceLog): Long {
        val logId = logDao.insert(log.toEntity())
        // Insert bản ghi history đầu tiên khi tạo log
        historyDao.insert(
            LogStatusHistoryEntity(
                logId           = logId,
                status          = when (log.status) {
                    MaintenanceStatus.RESOLVED      -> LogStatus.RESOLVED
                    MaintenanceStatus.WAITING_PARTS -> LogStatus.WAITING_PARTS
                    MaintenanceStatus.UNRESOLVED    -> LogStatus.UNRESOLVED
                },
                changedByUserId = log.userId,
                changedByName   = log.performedByName,
                note            = "",
                photoPaths      = gson.toJson(log.photoPaths),
                changedAt       = log.performedAt
            )
        )
        return logId
    }

    override suspend fun updateLog(log: MaintenanceLog) =
        logDao.update(log.toEntity())

    override suspend fun deleteLog(logId: Long) {
        val entity = logDao.findById(logId) ?: return
        logDao.delete(entity)
        // CASCADE tự xóa history theo FK
    }

    override suspend fun updateStatus(
        logId: Long,
        newStatus: MaintenanceStatus,
        changedByUserId: Long,
        changedByName: String,
        note: String,
        photoPaths: List<String>
    ) {
        // 1. Insert history record mới
        historyDao.insert(
            LogStatusHistoryEntity(
                logId           = logId,
                status          = when (newStatus) {
                    MaintenanceStatus.RESOLVED      -> LogStatus.RESOLVED
                    MaintenanceStatus.WAITING_PARTS -> LogStatus.WAITING_PARTS
                    MaintenanceStatus.UNRESOLVED    -> LogStatus.UNRESOLVED
                },
                changedByUserId = changedByUserId,
                changedByName   = changedByName,
                note            = note,
                photoPaths      = gson.toJson(photoPaths),
                changedAt       = System.currentTimeMillis()
            )
        )
        // 2. Sync currentStatus trên log chính
        logDao.updateStatus(logId, when (newStatus) {
            MaintenanceStatus.RESOLVED      -> LogStatus.RESOLVED
            MaintenanceStatus.WAITING_PARTS -> LogStatus.WAITING_PARTS
            MaintenanceStatus.UNRESOLVED    -> LogStatus.UNRESOLVED
        })
    }

    override suspend fun getKtvStats(userId: Long): KtvStats {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        return KtvStats(
            scannedToday = logDao.countScannedToday(userId, todayStart),
            pendingLogs  = logDao.countPending()
        )
    }

    override suspend fun getManagerStats(): ManagerStats {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val monthStart = cal.timeInMillis
        val (total, pending, expired) = Triple(
            deviceDao.count(),
            deviceDao.countWithPendingIssues(),
            deviceDao.countExpiredWarranty()
        )
        return ManagerStats(
            totalDevices         = total,
            pendingCount         = pending,
            expiredWarrantyCount = expired,
            logsThisMonth        = logDao.countInRange(monthStart, now)
        )
    }

    override suspend fun getLogsInRange(from: Long, to: Long): List<MaintenanceLog> =
        logDao.getLogsInRange(from, to).map { it.toDomain() }

    override suspend fun getWeeklyStats(monthStart: Long, monthEnd: Long): List<WeeklyLogCount> {
        val result = mutableListOf<WeeklyLogCount>()
        val prevMonthStart = monthStart - 30L * 24 * 60 * 60 * 1000
        val prevMonthEnd   = monthStart - 1
        val weekMs = 7L * 24 * 60 * 60 * 1000
        for (week in 1..5) {
            val wStart  = monthStart + (week - 1) * weekMs
            val wEnd    = minOf(wStart + weekMs - 1, monthEnd)
            val pwStart = prevMonthStart + (week - 1) * weekMs
            val pwEnd   = minOf(pwStart + weekMs - 1, prevMonthEnd)
            result.add(WeeklyLogCount(
                week     = week,
                current  = logDao.countInRange(wStart, wEnd),
                previous = logDao.countInRange(pwStart, pwEnd)
            ))
        }
        return result
    }

    override suspend fun getTopDevicesByIncidents(from: Long, to: Long): List<DeviceIncident> {
        return logDao.getTopDevicesByIncidents(from, to).mapNotNull { row ->
            val device = deviceDao.findById(row.deviceId)?.toDomain() ?: return@mapNotNull null
            DeviceIncident(device = device, count = row.cnt)
        }
    }

    override suspend fun getAllLogs(): List<MaintenanceLog> =
        logDao.getAll().map { it.toDomain() }
}
