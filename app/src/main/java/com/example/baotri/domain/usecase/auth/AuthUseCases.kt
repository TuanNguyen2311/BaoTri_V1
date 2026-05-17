package com.example.baotri.domain.usecase.auth

import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        if (username.isBlank()) return Result.failure(Exception("Tên đăng nhập không được để trống"))
        if (password.isBlank()) return Result.failure(Exception("Mật khẩu không được để trống"))
        val user = repo.login(username.trim(), password)
            ?: return Result.failure(Exception("Tên đăng nhập hoặc mật khẩu không đúng"))
        if (!user.isActive) return Result.failure(Exception("Tài khoản đã bị vô hiệu hóa"))
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

class ForgotPasswordUseCase @Inject constructor(private val repo: UserRepository) {
    suspend fun verifyPin(pin: String): Result<User> {
        if (pin.length != 6) return Result.failure(Exception("PIN phải là 6 chữ số"))
        // Find manager (admin is the only manager)
        val user = repo.getUserById(1L)
            ?: return Result.failure(Exception("Không tìm thấy tài khoản Quản lý"))
        val valid = repo.verifyPin(user.id, pin)
        return if (valid) Result.success(user)
        else Result.failure(Exception("PIN không đúng"))
    }

    suspend fun resetPassword(userId: Long, newPassword: String, confirmPassword: String): Result<Unit> {
        if (newPassword.length < 6)
            return Result.failure(Exception("Mật khẩu phải có ít nhất 6 ký tự"))
        if (newPassword != confirmPassword)
            return Result.failure(Exception("Mật khẩu xác nhận không khớp"))
        repo.changePassword(userId, newPassword)
        return Result.success(Unit)
    }
}
