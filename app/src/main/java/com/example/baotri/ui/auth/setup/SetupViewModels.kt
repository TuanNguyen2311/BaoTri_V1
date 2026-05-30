package com.example.baotri.ui.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.usecase.auth.ChangePasswordUseCase
import com.example.baotri.domain.usecase.auth.SetupPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Change Password State ─────────────────────────────────────
data class ChangePasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordStrength: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToSetupPin: Boolean = false
)

// ── Setup PIN State ───────────────────────────────────────────
data class SetupPinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val enteredPin: String = "",
    val step: PinStep = PinStep.ENTER,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToPinReveal: Boolean = false
)

data class PinRevealUiState(
    val pin: String = "",
    val navigateToDashboard: Boolean = false
)

enum class PinStep { ENTER, CONFIRM }

// ── ChangePasswordViewModel ───────────────────────────────────
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onNewPasswordChange(v: String) = _state.update {
        it.copy(newPassword = v, error = null, passwordStrength = computeStrength(v))
    }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }

    private fun computeStrength(pw: String): Int {
        var score = 0
        if (pw.length >= 6) score++
        if (pw.any { it.isUpperCase() }) score++
        if (pw.any { it.isLowerCase() }) score++
        if (pw.any { it.isDigit() }) score++
        return score
    }

    fun confirm(userId: Long) = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        changePasswordUseCase(userId, s.newPassword, s.confirmPassword)
            .onSuccess { _state.update { it.copy(isLoading = false, navigateToSetupPin = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }

    fun clearNav() = _state.update { it.copy(navigateToSetupPin = false) }
}

// ── SetupPinViewModel ─────────────────────────────────────────
@HiltViewModel
class SetupPinViewModel @Inject constructor(
    private val setupPinUseCase: SetupPinUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SetupPinUiState())
    val state: StateFlow<SetupPinUiState> = _state.asStateFlow()

    fun onDigitEntered(digit: String, userId: Long) {
        val current = _state.value
        when (current.step) {
            PinStep.ENTER -> {
                val newPin = (current.pin + digit).take(6)
                _state.update { it.copy(pin = newPin, error = null) }
                if (newPin.length == 6) {
                    _state.update { it.copy(enteredPin = newPin, step = PinStep.CONFIRM, pin = "", error = null) }
                }
            }
            PinStep.CONFIRM -> {
                val newConfirm = (current.confirmPin + digit).take(6)
                _state.update { it.copy(confirmPin = newConfirm, error = null) }
                if (newConfirm.length == 6) {
                    savePin(userId, newConfirm)
                }
            }
        }
    }

    fun onBackspace() {
        val s = _state.value
        when (s.step) {
            PinStep.ENTER   -> _state.update { it.copy(pin = s.pin.dropLast(1)) }
            PinStep.CONFIRM -> _state.update { it.copy(confirmPin = s.confirmPin.dropLast(1)) }
        }
    }

    private fun savePin(userId: Long, confirm: String) = viewModelScope.launch {
        val enteredPin = _state.value.enteredPin
        _state.update { it.copy(isLoading = true) }
        setupPinUseCase(userId, enteredPin, confirm)
            .onSuccess { _state.update { it.copy(isLoading = false, navigateToPinReveal = true) } }
            .onFailure { e ->
                _state.update { it.copy(
                    isLoading = false, error = e.message,
                    step = PinStep.ENTER, pin = "", confirmPin = "", enteredPin = ""
                )}
            }
    }

    fun clearNav() = _state.update { it.copy(navigateToPinReveal = false) }
}
