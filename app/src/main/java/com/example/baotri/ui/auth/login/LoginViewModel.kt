package com.example.baotri.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Role
import com.example.baotri.domain.model.User
import com.example.baotri.domain.usecase.auth.LoginUseCase
import com.example.baotri.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val selectedRole: Role = Role.TECHNICIAN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateTo: LoginNavEvent? = null
)

sealed class LoginNavEvent {
    data class ToChangePassword(val userId: Long, val role: Role) : LoginNavEvent()
    data class ToKtvDashboard(val userId: Long) : LoginNavEvent()
    data class ToManagerDashboard(val userId: Long) : LoginNavEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onRoleChange(role: Role)    = _state.update { it.copy(selectedRole = role) }

    fun login() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        loginUseCase(_state.value.username, _state.value.password)
            .onSuccess { user ->
                // Verify role matches selection
                if (user.role != _state.value.selectedRole) {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Vai trò không đúng. Vui lòng chọn lại."
                    )}
                    return@launch
                }
                session.saveSession(user.id, user.username, user.fullName, user.role.name)

                val navEvent = when {
                    user.isFirstLogin           -> LoginNavEvent.ToChangePassword(user.id, user.role)
                    user.role == Role.MANAGER   -> LoginNavEvent.ToManagerDashboard(user.id)
                    else                        -> LoginNavEvent.ToKtvDashboard(user.id)
                }
                _state.update { it.copy(isLoading = false, navigateTo = navEvent) }
            }
            .onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
    }

    fun clearNavEvent() = _state.update { it.copy(navigateTo = null) }
}
