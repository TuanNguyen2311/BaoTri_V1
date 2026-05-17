package com.example.baotri.domain.repository

import com.example.baotri.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(username: String, password: String): User?
    suspend fun getUserById(id: Long): User?
    suspend fun changePassword(userId: Long, newPassword: String)
    suspend fun setupPin(userId: Long, pin: String)
    suspend fun verifyPin(userId: Long, pin: String): Boolean
    suspend fun resetPasswordWithPin(userId: Long, pin: String, newPassword: String): Boolean
    fun getAllTechnicians(): Flow<List<User>>
    suspend fun createTechnician(username: String, password: String, fullName: String): Long
    suspend fun resetTechnicianPassword(userId: Long)
    suspend fun setTechnicianActive(userId: Long, active: Boolean)
    suspend fun deleteTechnician(userId: Long)
    suspend fun isFirstLogin(userId: Long): Boolean
    suspend fun hasManagerAccount(): Boolean
}

interface DeviceRepository {
    fun getAllDevices(): Flow<List<Device>>
    fun searchDevices(query: String): Flow<List<Device>>
    fun filterByArea(area: String): Flow<List<Device>>
    suspend fun getDeviceById(id: Long): Device?
    suspend fun getDeviceByCode(code: String): Device?
    suspend fun addDevice(device: Device): Long
    suspend fun updateDevice(device: Device)
    suspend fun deleteDevice(deviceId: Long)
    suspend fun updateQrPath(deviceId: Long, path: String)
    suspend fun getStats(): Triple<Int, Int, Int>  // total, pending, expiredWarranty
}

interface MaintenanceLogRepository {
    fun getLogsForDevice(deviceId: Long): Flow<List<MaintenanceLog>>
    fun getLogsByUser(userId: Long): Flow<List<MaintenanceLog>>
    fun getDraftsByUser(userId: Long): Flow<List<MaintenanceLog>>
    suspend fun getLogById(id: Long): MaintenanceLog?
    suspend fun saveLog(log: MaintenanceLog): Long
    suspend fun updateLog(log: MaintenanceLog)
    suspend fun deleteLog(logId: Long)
    suspend fun getKtvStats(userId: Long): KtvStats
    suspend fun getManagerStats(): ManagerStats
    suspend fun getLogsInRange(from: Long, to: Long): List<MaintenanceLog>
    suspend fun getWeeklyStats(monthStart: Long, monthEnd: Long): List<WeeklyLogCount>
    suspend fun getTopDevicesByIncidents(from: Long, to: Long): List<DeviceIncident>
    suspend fun getAllLogs(): List<MaintenanceLog>
}

interface BackupRepository {
    suspend fun exportBackup(): ByteArray       // Encrypted .btdb bytes
    suspend fun importBackup(data: ByteArray)   // Decrypt & restore
    fun getBackupHistory(): Flow<List<BackupHistory>>
    suspend fun getLatestBackup(): BackupHistory?
    suspend fun recordBackup(action: String, fileName: String, sizeBytes: Long, destination: String)
}
