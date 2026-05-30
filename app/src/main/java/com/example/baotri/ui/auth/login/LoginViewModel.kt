package com.example.baotri.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Role
import com.example.baotri.domain.usecase.auth.ClearRememberedUsernameUseCase
import com.example.baotri.domain.usecase.auth.GetRememberedUsernameUseCase
import com.example.baotri.domain.usecase.auth.LoginUseCase
import com.example.baotri.domain.usecase.auth.SaveRememberedUsernameUseCase
import com.example.baotri.domain.usecase.auth.SaveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
) {
    val isFormValid: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

sealed class LoginNavEvent {
    data class ToChangePassword(val userId: Long, val role: Role) : LoginNavEvent()
    object ToKtvDashboard : LoginNavEvent()
    object ToManagerDashboard : LoginNavEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val saveSession: SaveSessionUseCase,
    private val getRememberedUsername: GetRememberedUsernameUseCase,
    private val saveRememberedUsername: SaveRememberedUsernameUseCase,
    private val clearRememberedUsername: ClearRememberedUsernameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _navChannel = Channel<LoginNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<LoginNavEvent> = _navChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            getRememberedUsername().first().let { saved ->
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

        loginUseCase(s.username.trim(), s.password)
            .onSuccess { user ->
                saveSession(user.id, user.username, user.fullName, user.role.name)
                if (s.rememberMe) saveRememberedUsername(user.username)
                else clearRememberedUsername()

                val event = when {
                    user.isFirstLogin         -> LoginNavEvent.ToChangePassword(user.id, user.role)
                    user.role == Role.MANAGER -> LoginNavEvent.ToManagerDashboard
                    else                      -> LoginNavEvent.ToKtvDashboard
                }
                _navChannel.send(event)
            }
            .onFailure { e ->
                _state.update { it.copy(isLoading = false, generalError = e.message) }
            }
    }
}