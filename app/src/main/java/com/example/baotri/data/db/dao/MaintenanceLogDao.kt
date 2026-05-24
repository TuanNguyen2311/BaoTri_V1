package com.example.baotri.data.db.dao

import androidx.room.*
import com.example.baotri.data.model.MaintenanceLogEntity
import com.example.baotri.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceLogDao {

    @Query("""
        SELECT * FROM maintenance_logs 
        WHERE deviceId = :deviceId AND isDraft = 0
        ORDER BY performedAt DESC
    """)
    fun getLogsForDevice(deviceId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("""
        SELECT * FROM maintenance_logs
        WHERE userId = :userId AND isDraft = 0
        ORDER BY performedAt DESC
    """)
    fun getLogsByUser(userId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs WHERE userId = :userId AND isDraft = 1")
    fun getDraftsByUser(userId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MaintenanceLogEntity?

    // Latest log per device (for status display)
    @Query("""
        SELECT * FROM maintenance_logs 
        WHERE deviceId = :deviceId AND isDraft = 0
        ORDER BY performedAt DESC LIMIT 1
    """)
    suspend fun getLatestForDevice(deviceId: Long): MaintenanceLogEntity?

    // Stats queries
    @Query("""
        SELECT COUNT(*) FROM maintenance_logs 
        WHERE performedAt >= :from AND performedAt <= :to AND isDraft = 0
    """)
    suspend fun countInRange(from: Long, to: Long): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs 
        WHERE status = 'WAITING_PARTS' OR status = 'UNRESOLVED' AND isDraft = 0
    """)
    suspend fun countPending(): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE status = 'UNRESOLVED' AND isDraft = 0
    """)
    suspend fun countUnresolved(): Int

    @Query("""
        SELECT COUNT(*) FROM maintenance_logs
        WHERE status = 'RESOLVED' AND performedAt >= :from AND performedAt <= :to AND isDraft = 0
    """)
    suspend fun countResolvedInRange(from: Long, to: Long): Int

    // For report - logs grouped by week
    @Query("""
        SELECT * FROM maintenance_logs
        WHERE performedAt >= :from AND performedAt <= :to AND isDraft = 0
        ORDER BY performedAt DESC
    """)
    suspend fun getLogsInRange(from: Long, to: Long): List<MaintenanceLogEntity>

    // Top devices by incident count
    @Query("""
        SELECT deviceId, COUNT(*) as cnt FROM maintenance_logs
        WHERE performedAt >= :from AND performedAt <= :to AND isDraft = 0
        GROUP BY deviceId ORDER BY cnt DESC LIMIT 5
    """)
    suspend fun getTopDevicesByIncidents(from: Long, to: Long): List<DeviceIncidentCount>

    // Today's scan count for KTV
    @Query("""
        SELECT COUNT(DISTINCT deviceId) FROM maintenance_logs
        WHERE userId = :userId AND performedAt >= :todayStart AND isDraft = 0
    """)
    suspend fun countScannedToday(userId: Long, todayStart: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MaintenanceLogEntity): Long

    @Update
    suspend fun update(log: MaintenanceLogEntity)

    @Delete
    suspend fun delete(log: MaintenanceLogEntity)

    @Query("SELECT * FROM maintenance_logs ORDER BY createdAt DESC")
    suspend fun getAll(): List<MaintenanceLogEntity>

    // Sync currentStatus khi có cập nhật trạng thái mới
    @Query("UPDATE maintenance_logs SET status = :newStatus WHERE id = :logId")
    suspend fun updateStatus(logId: Long, newStatus: LogStatus)
}

data class DeviceIncidentCount(
    val deviceId: Long,
    val cnt: Int
)
