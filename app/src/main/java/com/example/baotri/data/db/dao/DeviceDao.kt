package com.example.baotri.data.db.dao

import androidx.room.*
import com.example.baotri.data.model.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DeviceEntity?

    @Query("SELECT * FROM devices WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): DeviceEntity?

    @Query("""
        SELECT * FROM devices 
        WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun search(query: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE location LIKE '%' || :area || '%' ORDER BY name ASC")
    fun filterByArea(area: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY id ASC")
    suspend fun getAllForBackup(): List<DeviceEntity>

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun count(): Int

    @Query("""
        SELECT COUNT(*) FROM devices d
        INNER JOIN maintenance_logs l ON l.deviceId = d.id
        WHERE l.status = 'WAITING_PARTS' OR l.status = 'UNRESOLVED'
    """)
    suspend fun countWithPendingIssues(): Int

    @Query("SELECT COUNT(*) FROM devices WHERE warrantyDate < :now AND warrantyDate IS NOT NULL")
    suspend fun countExpiredWarranty(now: Long = System.currentTimeMillis()): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(device: DeviceEntity): Long

    @Update
    suspend fun update(device: DeviceEntity)

    @Delete
    suspend fun delete(device: DeviceEntity)

    @Query("UPDATE devices SET qrPath = :path WHERE id = :id")
    suspend fun updateQrPath(id: Long, path: String)
}
