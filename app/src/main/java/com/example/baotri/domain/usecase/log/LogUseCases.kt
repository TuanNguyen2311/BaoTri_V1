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

class GetLogWithHistoryUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(logId: Long): MaintenanceLog? =
        repo.getLogWithHistory(logId)
}

class GetStatusHistoryUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    operator fun invoke(logId: Long): Flow<List<LogStatusHistory>> =
        repo.getStatusHistory(logId)
}

class UpdateLogStatusUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(
        logId: Long,
        newStatus: MaintenanceStatus,
        changedByUserId: Long,
        changedByName: String,
        note: String,
        photoPaths: List<String>
    ): Result<Unit> {
        // Kiểm tra log có thể cập nhật không
        val log = repo.getLogById(logId)
            ?: return Result.failure(Exception("Không tìm thấy log"))
        if (log.isDraft)
            return Result.failure(Exception("Log nháp chưa thể cập nhật trạng thái"))
        if (log.status == MaintenanceStatus.RESOLVED)
            return Result.failure(Exception("Log đã hoàn thành, không thể cập nhật"))
        return try {
            repo.updateStatus(logId, newStatus, changedByUserId, changedByName, note, photoPaths)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
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

class GetWeeklyStatsUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(monthStart: Long, monthEnd: Long): List<WeeklyLogCount> =
        repo.getWeeklyStats(monthStart, monthEnd)
}

class GetTopDevicesUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(from: Long, to: Long): List<DeviceIncident> =
        repo.getTopDevicesByIncidents(from, to)
}

class GetReportSummaryUseCase @Inject constructor(private val repo: MaintenanceLogRepository) {
    suspend operator fun invoke(from: Long, to: Long): Map<String, Int> {
        val logs = repo.getLogsInRange(from, to)
        return mapOf(
            "total"    to logs.size,
            "resolved" to logs.count { it.status == MaintenanceStatus.RESOLVED },
            "waiting"  to logs.count { it.status == MaintenanceStatus.WAITING_PARTS },
            "urgent"   to logs.count { it.status == MaintenanceStatus.UNRESOLVED }
        )
    }
}
