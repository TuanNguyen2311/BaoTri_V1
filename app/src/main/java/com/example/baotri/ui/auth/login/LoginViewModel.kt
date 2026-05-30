package com.example.baotri.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Role
import com.example.baotri.domain.repository.SessionRepository
import com.example.baotri.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    // showPassword nằm trong State để survive xoay màn hình
    val showPassword: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    // Field-level errors
    val usernameError: String? = null,
    val passwordError: String? = null,
    // General error (snackbar)
    val generalError: String? = null
) {
    val isFormValid: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

// ── One-shot Navigation Events — dùng Channel để tránh re-trigger ──
sealed class LoginNavEvent {
    data class ToChangePassword(val userId: Long, val role: Role) : LoginNavEvent()
    object ToKtvDashboard  : LoginNavEvent()   // userId lấy từ SessionManager
    object ToManagerDashboard : LoginNavEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val session: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // Channel đảm bảo mỗi event chỉ consume 1 lần
    private val _navChannel = Channel<LoginNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<LoginNavEvent> = _navChannel.receiveAsFlow()

    init {
        // Khôi phục username nếu đã chọn "Ghi nhớ đăng nhập"
        viewModelScope.launch {
            session.rememberedUsername.first().let { saved ->
                if (saved.isNotBlank()) {
                    _state.update { it.copy(username = saved, rememberMe = true) }
                }
            }
        }
    }

    fun onUsernameChange(v: String) =
        _state.update { it.copy(username = v, usernameError = null, generalError = null) }

    fun onPasswordChange(v: String) =
        _state.update { it.copy(password = v, passwordError = null, generalError = null) }

    fun onTogglePassword() =
        _state.update { it.copy(showPassword = !it.showPassword) }

    fun onRememberMeChange(v: Boolean) =
        _state.update { it.copy(rememberMe = v) }

    fun clearGeneralError() =
        _state.update { it.copy(generalError = null) }

    fun login() = viewModelScope.launch {
        val s = _state.value

        // ── Client-side validation trước khi gọi UseCase ──
        var hasError = false
        if (s.username.isBlank()) {
            _state.update { it.copy(usernameError = "Vui lòng nhập tên đăng nhập") }
            hasError = true
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "Vui lòng nhập mật khẩu") }
            hasError = true
        }
        if (hasError) return@launch

        _state.update { it.copy(isLoading = true, generalError = null) }

        // ── UseCase xử lý login + tự nhận diện role ────────
        // Không cần truyền role — domain tự trả về role từ DB
        loginUseCase(s.username.trim(), s.password)
            .onSuccess { user ->
                // Lưu session
                session.saveSession(user.id, user.username, user.fullName, user.role.name)

                // Ghi nhớ đăng nhập nếu người dùng chọn
                if (s.rememberMe) session.saveRememberedUsername(user.username)
                else session.clearRememberedUsername()

                // Điều hướng qua Channel (one-shot, không bị trigger lại)
                val event = when {
                    user.isFirstLogin          -> LoginNavEvent.ToChangePassword(user.id, user.role)
                    user.role == Role.MANAGER  -> LoginNavEvent.ToManagerDashboard
                    else                       -> LoginNavEvent.ToKtvDashboard
                }
                _navChannel.send(event)
            }
            .onFailure { e ->
                _state.update { it.copy(isLoading = false, generalError = e.message) }
            }
    }
}
