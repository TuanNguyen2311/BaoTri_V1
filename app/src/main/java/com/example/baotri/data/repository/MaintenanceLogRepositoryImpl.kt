package com.example.baotri.data.repository

import com.example.baotri.data.db.dao.DeviceDao
import com.example.baotri.data.db.dao.MaintenanceLogDao
import com.example.baotri.domain.model.*
import com.example.baotri.domain.repository.MaintenanceLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class MaintenanceLogRepositoryImpl @Inject constructor(
    private val logDao: MaintenanceLogDao,
    private val deviceDao: DeviceDao
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

    override suspend fun saveLog(log: MaintenanceLog): Long =
        logDao.insert(log.toEntity())

    override suspend fun updateLog(log: MaintenanceLog) =
        logDao.update(log.toEntity())

    override suspend fun deleteLog(logId: Long) {
        val entity = logDao.findById(logId) ?: return
        logDao.delete(entity)
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
            val wStart = monthStart + (week - 1) * weekMs
            val wEnd   = minOf(wStart + weekMs - 1, monthEnd)
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
