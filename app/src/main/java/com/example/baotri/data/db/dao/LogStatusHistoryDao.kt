package com.example.baotri.data.db.dao

import androidx.room.*
import com.example.baotri.data.model.LogStatusHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogStatusHistoryDao {

    @Query("SELECT * FROM log_status_history WHERE logId = :logId ORDER BY changedAt ASC")
    fun getHistoryForLog(logId: Long): Flow<List<LogStatusHistoryEntity>>

    @Query("SELECT * FROM log_status_history WHERE logId = :logId ORDER BY changedAt ASC")
    suspend fun getHistoryForLogOnce(logId: Long): List<LogStatusHistoryEntity>

    @Query("SELECT * FROM log_status_history WHERE logId = :logId ORDER BY changedAt DESC LIMIT 1")
    suspend fun getLatestForLog(logId: Long): LogStatusHistoryEntity?

    @Insert
    suspend fun insert(history: LogStatusHistoryEntity): Long

    @Query("DELETE FROM log_status_history WHERE logId = :logId")
    suspend fun deleteAllForLog(logId: Long)

    // Dùng cho backup
    @Query("SELECT * FROM log_status_history ORDER BY changedAt ASC")
    suspend fun getAll(): List<LogStatusHistoryEntity>
}
