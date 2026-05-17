package com.example.baotri.data.db.dao

import androidx.room.*
import com.example.baotri.data.model.BackupHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupHistoryDao {

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC LIMIT 20")
    fun getRecent(): Flow<List<BackupHistoryEntity>>

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): BackupHistoryEntity?

    @Insert
    suspend fun insert(history: BackupHistoryEntity): Long

    @Query("DELETE FROM backup_history WHERE id NOT IN (SELECT id FROM backup_history ORDER BY createdAt DESC LIMIT 20)")
    suspend fun pruneOld()
}
