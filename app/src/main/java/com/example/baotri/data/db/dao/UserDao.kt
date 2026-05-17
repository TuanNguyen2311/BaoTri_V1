package com.example.baotri.data.db.dao

import androidx.room.*
import com.example.baotri.data.model.UserEntity
import com.example.baotri.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'TECHNICIAN' ORDER BY fullName ASC")
    fun getAllTechnicians(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM users WHERE role = 'MANAGER'")
    suspend fun countManagers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :hash, isFirstLogin = 0 WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String)

    @Query("UPDATE users SET pinHash = :hash WHERE id = :id")
    suspend fun updatePin(id: Long, hash: String)

    @Query("UPDATE users SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Delete
    suspend fun delete(user: UserEntity)
}
