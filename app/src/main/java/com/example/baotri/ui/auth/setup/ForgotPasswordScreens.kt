package com.example.baotri.ui.auth.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.usecase.auth.ForgotPasswordUseCase
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── States ────────────────────────────────────────────────────
enum class ForgotStep { ENTER_PIN, RESET_PASSWORD }

data class ForgotPasswordUiState(
    val step: ForgotStep = ForgotStep.ENTER_PIN,
    val pin: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val verifiedUserId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pinAttempts: Int = 0,
    val success: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val useCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onPinDigit(digit: String) {
        val current = _state.value.pin
        val newPin = (current + digit).take(6)
        _state.update { it.copy(pin = newPin, error = null) }
        if (newPin.length == 6) verifyPin(newPin)
    }

    fun onPinBackspace() {
        _state.update { it.copy(pin = it.pin.dropLast(1)) }
    }

    private fun verifyPin(pin: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        useCase.verifyPin(pin)
            .onSuccess { user ->
                _state.update { it.copy(
                    isLoading = false,
                    step = ForgotStep.RESET_PASSWORD,
                    verifiedUserId = user.id,
                    pin = ""
                )}
            }
            .onFailure { e ->
                val attempts = _state.value.pinAttempts + 1
                _state.update { it.copy(
                    isLoading = false,
                    pin = "",
                    pinAttempts = attempts,
                    error = if (attempts >= 3)
                        "PIN không đúng. Đã thử 3 lần. Vui lòng liên hệ hỗ trợ."
                    else
                        "PIN không đúng. Còn ${3 - attempts} lần thử."
                )}
            }
    }

    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }

    fun resetPassword() = viewModelScope.launch {
        val s = _state.value
        val userId = s.verifiedUserId ?: return@launch
        _state.update { it.copy(isLoading = true, error = null) }
        useCase.resetPassword(userId, s.newPassword, s.confirmPassword)
            .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }
}

// ── Screens ───────────────────────────────────────────────────

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    vm: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onNavigateToLogin()
    }

    when (state.step) {
        ForgotStep.ENTER_PIN -> EnterPinScreen(
            state = state, vm = vm, onBack = onNavigateBack
        )
        ForgotStep.RESET_PASSWORD -> ResetPasswordScreen(
            state = state, vm = vm
        )
    }
}

@Composable
private fun EnterPinScreen(
    state: ForgotPasswordUiState,
    vm: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Quên mật khẩu?",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Nhập mã PIN 6 số đã tạo lúc setup để xác thực danh tính và đặt lại mật khẩu mới.",
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text("Nhập mã PIN khẩn cấp",
            style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        PinDots(filled = state.pin.length, total = 6, color = PurplePrimary)
        Spacer(Modifier.height(8.dp))
        if (state.error != null) {
            Text(state.error!!, color = RedColor, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        if (state.isLoading) {
            CircularProgressIndicator(color = PurplePrimary, modifier = Modifier.size(28.dp))
        } else {
            val locked = state.pinAttempts >= 3
            PinNumpad(
                onDigit = { if (!locked) vm.onPinDigit(it) },
                onBackspace = vm::onPinBackspace
            )
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    state: ForgotPasswordUiState,
    vm: ForgotPasswordViewModel
) {
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        InfoBox(
            message = "PIN xác thực thành công. Bạn có thể đặt mật khẩu mới cho tài khoản Quản lý.",
            icon = Icons.Default.CheckCircle,
            type = InfoBoxType.SUCCESS
        )
        Spacer(Modifier.height(20.dp))

        Text("Đặt mật khẩu mới",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Mật khẩu mới phải khác mật khẩu cũ.",
            color = TextSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        Text("Mật khẩu mới", style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = state.newPassword, onValueChange = vm::onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextTertiary) },
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextTertiary)
                }
            },
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
        )

        Spacer(Modifier.height(14.dp))

        Text("Xác nhận mật khẩu mới", style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = state.confirmPassword, onValueChange = vm::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = TextTertiary) },
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextTertiary)
                }
            },
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
        )

        Spacer(Modifier.height(14.dp))

        InfoBox(
            message = "Mã PIN khẩn cấp vẫn được giữ nguyên. Bạn có thể đổi PIN trong Cài đặt bất cứ lúc nào.",
            icon = Icons.Default.Info, type = InfoBoxType.INFO
        )

        Spacer(Modifier.height(16.dp))

        if (state.error != null) {
            Text(state.error!!, color = RedColor, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        LoadingButton(
            text = "Xác nhận & Đăng nhập",
            loading = state.isLoading,
            onClick = vm::resetPassword,
            modifier = Modifier.fillMaxWidth(),
            containerColor = GreenPrimary
        )
    }
}
