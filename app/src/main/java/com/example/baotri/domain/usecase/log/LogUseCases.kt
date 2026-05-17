package com.example.baotri.domain.usecase.log

import com.example.baotri.domain.model.*
import com.example.baotri.domain.repository.MaintenanceLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeviceLogsUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    operator fun invoke(deviceId: Long): Flow<List<MaintenanceLog>> =
        repo.getLogsForDevice(deviceId)
}

class GetUserLogsUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    operator fun invoke(userId: Long): Flow<List<MaintenanceLog>> =
        repo.getLogsByUser(userId)
}

class SaveLogUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(log: MaintenanceLog): Result<Long> {
        if (log.description.isBlank())
            return Result.failure(Exception("Mô tả sự cố không được để trống"))
        if (log.solution.isBlank() && !log.isDraft)
            return Result.failure(Exception("Cách xử lý không được để trống"))
        return try {
            val id = if (log.id == 0L) repo.saveLog(log)
                     else { repo.updateLog(log); log.id }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetKtvStatsUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(userId: Long): KtvStats = repo.getKtvStats(userId)
}

class GetManagerStatsUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(): ManagerStats = repo.getManagerStats()
}

class GetReportUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend fun getWeeklyStats(monthStart: Long, monthEnd: Long): List<WeeklyLogCount> =
        repo.getWeeklyStats(monthStart, monthEnd)

    suspend fun getTopDevices(from: Long, to: Long): List<DeviceIncident> =
        repo.getTopDevicesByIncidents(from, to)

    suspend fun getSummary(from: Long, to: Long): Map<String, Int> {
        val logs = repo.getLogsInRange(from, to)
        return mapOf(
            "total"    to logs.size,
            "resolved" to logs.count { it.status == MaintenanceStatus.RESOLVED },
            "waiting"  to logs.count { it.status == MaintenanceStatus.WAITING_PARTS },
            "urgent"   to logs.count { it.status == MaintenanceStatus.UNRESOLVED }
        )
    }
}
