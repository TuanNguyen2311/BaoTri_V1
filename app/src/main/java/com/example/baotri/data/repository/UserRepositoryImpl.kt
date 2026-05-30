package com.example.baotri.data.repository

import com.example.baotri.data.db.dao.UserDao
import com.example.baotri.data.model.UserEntity
import com.example.baotri.data.model.UserRole
import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.UserRepository
import com.example.baotri.util.SecurityUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao
) : UserRepository {

    override suspend fun login(username: String, password: String): User? {
        val entity = dao.findByUsername(username) ?: return null
        if (!SecurityUtil.verifyHash(password, entity.passwordHash)) return null
        return entity.toDomain()
    }

    override suspend fun getUserById(id: Long): User? =
        dao.findById(id)?.toDomain()

    // ← Thêm mới: tìm user theo username, dùng cho ForgotPassword
    override suspend fun getUserByUsername(username: String): User? =
        dao.findByUsername(username)?.toDomain()

    override suspend fun changePassword(userId: Long, newPassword: String) {
        dao.updatePassword(userId, SecurityUtil.sha256(newPassword))
    }

    override suspend fun setupPin(userId: Long, pin: String) {
        dao.updatePin(userId, SecurityUtil.sha256(pin))
    }

    override suspend fun verifyPin(userId: Long, pin: String): Boolean {
        val entity = dao.findById(userId) ?: return false
        val pinHash = entity.pinHash ?: return false
        return SecurityUtil.verifyHash(pin, pinHash)
    }

    override suspend fun resetPasswordWithPin(
        userId: Long,
        pin: String,
        newPassword: String
    ): Boolean {
        if (!verifyPin(userId, pin)) return false
        changePassword(userId, newPassword)
        return true
    }

    override fun getAllTechnicians(): Flow<List<User>> =
        dao.getAllTechnicians().map { list -> list.map { it.toDomain() } }

    override suspend fun createTechnician(
        username: String,
        password: String,
        fullName: String
    ): Long {
        val entity = UserEntity(
            username     = username.lowercase().trim(),
            passwordHash = SecurityUtil.sha256(password),
            role         = UserRole.TECHNICIAN,
            fullName     = fullName,
            isActive     = true,
            isFirstLogin = true
        )
        return dao.insert(entity)
    }

    override suspend fun resetTechnicianPassword(userId: Long, newPassword: String) {
        dao.updatePassword(userId, SecurityUtil.sha256(newPassword))
    }

    override suspend fun setTechnicianActive(userId: Long, active: Boolean) {
        dao.setActive(userId, active)
    }

    override suspend fun deleteTechnician(userId: Long) {
        val entity = dao.findById(userId) ?: return
        dao.delete(entity)
    }

    override suspend fun isFirstLogin(userId: Long): Boolean =
        dao.findById(userId)?.isFirstLogin ?: false

    override suspend fun hasManagerAccount(): Boolean =
        dao.countManagers() > 0
}