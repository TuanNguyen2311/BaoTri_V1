package com.example.baotri.ui.auth.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.usecase.auth.ResetPasswordAfterPinUseCase
import com.example.baotri.domain.usecase.auth.VerifyPinWithUsernameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotStep { ENTER_PIN, RESET_PASSWORD }

data class ForgotPasswordUiState(
    val username: String = "",
    val step: ForgotStep = ForgotStep.ENTER_PIN,
    val pin: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordStrength: Int = 0,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val verifiedUserId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pinAttempts: Int = 0,
    val success: Boolean = false
) {
    val isPinLocked: Boolean get() = pinAttempts >= 3
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyPin: VerifyPinWithUsernameUseCase,
    private val resetPassword: ResetPasswordAfterPinUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    init {
        val username = savedStateHandle.get<String>("username") ?: ""
        _state.update { it.copy(username = username) }
    }

    fun onPinDigit(digit: String) {
        val s = _state.value
        if (s.isPinLocked || s.isLoading) return
        val newPin = (s.pin + digit).take(6)
        _state.update { it.copy(pin = newPin, error = null) }
        if (newPin.length == 6) doVerifyPin(newPin)
    }

    fun onPinBackspace() = _state.update { it.copy(pin = it.pin.dropLast(1)) }

    private fun doVerifyPin(pin: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        verifyPin(_state.value.username, pin)
            .onSuccess { user ->
                _state.update {
                    it.copy(isLoading = false, step = ForgotStep.RESET_PASSWORD,
                        verifiedUserId = user.id, pin = "")
                }
            }
            .onFailure { e ->
                val attempts = _state.value.pinAttempts + 1
                _state.update {
                    it.copy(
                        isLoading   = false,
                        pin         = "",
                        pinAttempts = attempts,
                        error       = if (attempts >= 3)
                            "Sai PIN 3 lần. Vui lòng liên hệ quản trị viên."
                        else
                            "${e.message ?: "PIN không đúng"}. Còn ${3 - attempts} lần thử."
                    )
                }
            }
    }

    fun onNewPasswordChange(v: String) = _state.update {
        it.copy(newPassword = v, error = null, passwordStrength = computeStrength(v))
    }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }
    fun onToggleNewPassword()              = _state.update { it.copy(showNewPassword = !it.showNewPassword) }
    fun onToggleConfirmPassword()          = _state.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }

    private fun computeStrength(pw: String): Int {
        var score = 0
        if (pw.length >= 6) score++
        if (pw.any { it.isUpperCase() }) score++
        if (pw.any { it.isLowerCase() }) score++
        if (pw.any { it.isDigit() }) score++
        return score
    }

    fun doResetPassword() = viewModelScope.launch {
        val s = _state.value
        val userId = s.verifiedUserId ?: return@launch
        _state.update { it.copy(isLoading = true, error = null) }
        resetPassword(userId, s.newPassword, s.confirmPassword)
            .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }
}
