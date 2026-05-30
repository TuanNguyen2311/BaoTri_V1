package com.example.baotri.domain.usecase.auth

import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.SessionRepository
import com.example.baotri.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogoutUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke() = sessionRepository.clearSession()
}

class SaveSessionUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke(userId: Long, username: String, fullName: String, role: String) =
        sessionRepository.saveSession(userId, username, fullName, role)
}

class GetRememberedUsernameUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    operator fun invoke(): Flow<String> = sessionRepository.rememberedUsername
}

class SaveRememberedUsernameUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke(username: String) = sessionRepository.saveRememberedUsername(username)
}

class ClearRememberedUsernameUseCase @Inject constructor(private val sessionRepository: SessionRepository) {
    suspend operator fun invoke() = sessionRepository.clearRememberedUsername()
}

class LoginUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        val user = repo.login(username.trim().lowercase(), password)
            ?: return Result.failure(Exception("Tên đăng nhập hoặc mật khẩu không đúng"))
        if (!user.isActive)
            return Result.failure(Exception("Tài khoản đã bị vô hiệu hóa. Liên hệ Quản lý."))
        return Result.success(user)
    }
}

class ChangePasswordUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(userId: Long, newPassword: String, confirmPassword: String): Result<Unit> {
        if (newPassword.length < 6)
            return Result.failure(Exception("Mật khẩu phải có ít nhất 6 ký tự"))
        if (newPassword != confirmPassword)
            return Result.failure(Exception("Mật khẩu xác nhận không khớp"))
        if (!newPassword.any { it.isUpperCase() } || !newPassword.any { it.isLowerCase() })
            return Result.failure(Exception("Mật khẩu phải có chữ hoa và chữ thường"))
        repo.changePassword(userId, newPassword)
        return Result.success(Unit)
    }
}

class SetupPinUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(userId: Long, pin: String, confirmPin: String): Result<Unit> {
        if (pin.length != 6 || !pin.all { it.isDigit() })
            return Result.failure(Exception("PIN phải là 6 chữ số"))
        if (pin != confirmPin)
            return Result.failure(Exception("PIN xác nhận không khớp"))
        repo.setupPin(userId, pin)
        return Result.success(Unit)
    }
}

class VerifyPinWithUsernameUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(username: String, pin: String): Result<User> {
        if (username.isBlank())
            return Result.failure(Exception("Tên đăng nhập không được để trống"))
        if (pin.length != 6)
            return Result.failure(Exception("PIN phải là 6 chữ số"))
        val user = repo.getUserByUsername(username.trim().lowercase())
            ?: return Result.failure(Exception("Không tìm thấy tài khoản \"$username\""))
        if (!user.isActive)
            return Result.failure(Exception("Tài khoản đã bị vô hiệu hóa"))
        val valid = repo.verifyPin(user.id, pin)
        return if (valid) Result.success(user)
        else Result.failure(Exception("PIN không đúng"))
    }
}

class ResetPasswordAfterPinUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(
        userId: Long,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        if (newPassword.length < 6)
            return Result.failure(Exception("Mật khẩu phải có ít nhất 6 ký tự"))
        if (newPassword != confirmPassword)
            return Result.failure(Exception("Mật khẩu xác nhận không khớp"))
        repo.changePassword(userId, newPassword)
        return Result.success(Unit)
    }
}