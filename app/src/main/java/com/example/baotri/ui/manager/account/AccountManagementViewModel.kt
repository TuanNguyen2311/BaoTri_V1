package com.example.baotri.ui.manager.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.User
import com.example.baotri.domain.usecase.account.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val technicians: List<User> = emptyList(),
    val showAddDialog: Boolean = false,
    val newUsername: String = "",
    val newFullName: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val getAllTechnicians: GetAllTechniciansUseCase,
    private val createTechnician: CreateTechnicianUseCase,
    private val resetPassword: ResetTechnicianPasswordUseCase,
    private val toggleActive: ToggleTechnicianActiveUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getAllTechnicians().collect { list ->
                _state.update { it.copy(technicians = list) }
            }
        }
    }

    fun showAddDialog() = _state.update { it.copy(showAddDialog = true, error = null) }
    fun dismissDialog() = _state.update {
        it.copy(showAddDialog = false, newUsername = "", newFullName = "", newPassword = "", error = null)
    }
    fun onUsernameChange(v: String) = _state.update { it.copy(newUsername = v) }
    fun onFullNameChange(v: String) = _state.update { it.copy(newFullName = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(newPassword = v) }

    fun addTechnician() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        createTechnician(s.newUsername, s.newPassword, s.newFullName)
            .onSuccess {
                _state.update {
                    it.copy(isLoading = false, showAddDialog = false,
                        newUsername = "", newFullName = "", newPassword = "")
                }
            }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun resetPassword(userId: Long) = viewModelScope.launch {
        resetPassword.invoke(userId)
    }

    fun toggleActive(userId: Long, active: Boolean) = viewModelScope.launch {
        toggleActive.invoke(userId, active)
    }
}
