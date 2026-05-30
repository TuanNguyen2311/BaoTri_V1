package com.example.baotri.domain.usecase.account

import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.DEFAULT_TECHNICIAN_PASSWORD
import com.example.baotri.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTechniciansUseCase @Inject constructor(private val repo: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repo.getAllTechnicians()
}

class CreateTechnicianUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(
        username: String,
        password: String,
        fullName: String
    ): Result<Long> {
        if (fullName.isBlank())
            return Result.failure(Exception("Vui lòng nhập họ tên"))
        if (username.isBlank())
            return Result.failure(Exception("Vui lòng nhập tên đăng nhập"))
        return try {
            val id = repo.createTechnician(username, password.ifBlank { DEFAULT_TECHNICIAN_PASSWORD }, fullName)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ResetTechnicianPasswordUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(userId: Long): Result<Unit> = try {
        repo.resetTechnicianPassword(userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class ToggleTechnicianActiveUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(userId: Long, currentlyActive: Boolean): Result<Unit> = try {
        repo.setTechnicianActive(userId, !currentlyActive)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
