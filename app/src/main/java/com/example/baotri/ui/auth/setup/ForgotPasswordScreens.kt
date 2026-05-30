package com.example.baotri.ui.auth.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

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
        ForgotStep.ENTER_PIN      -> EnterPinScreen(state = state, vm = vm, onBack = onNavigateBack)
        ForgotStep.RESET_PASSWORD -> ResetPasswordScreen(state = state, vm = vm)
    }
}

// ── Bước 1: Nhập PIN ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterPinScreen(
    state: ForgotPasswordUiState,
    vm: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại đăng nhập")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Nhập mã PIN 6 số gắn với tài khoản để xác thực và đặt lại mật khẩu.",
                color = TextSecondary, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Hiển thị username đang xác thực
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Person, null,
                    tint = GreenPrimary, modifier = Modifier.size(20.dp))
                Column {
                    Text("Tài khoản", fontSize = 11.sp, color = TextTertiary)
                    Text(
                        state.username.ifBlank { "(chưa nhập)" },
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (state.username.isBlank()) TextTertiary
                                else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Nhập mã PIN khẩn cấp",
                style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(14.dp))

            PinDots(filled = state.pin.length, total = 6, color = PurplePrimary)

            Spacer(Modifier.height(10.dp))

            if (state.error != null) {
                Text(
                    state.error,
                    color = if (state.isPinLocked) RedColor else AmberColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = PurplePrimary, modifier = Modifier.size(32.dp))
            } else if (state.isPinLocked) {
                InfoBox(
                    message = "Tài khoản bị khóa xác thực PIN. Vui lòng liên hệ quản trị viên để được hỗ trợ.",
                    icon    = Icons.Default.Lock,
                    type    = InfoBoxType.ERROR
                )
            } else {
                PinNumpad(onDigit = vm::onPinDigit, onBackspace = vm::onPinBackspace)
            }
        }
    }
}

// ── Bước 2: Đặt mật khẩu mới ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordScreen(
    state: ForgotPasswordUiState,
    vm: ForgotPasswordViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt mật khẩu mới", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            InfoBox(
                message = "PIN xác thực thành công cho tài khoản \"${state.username}\". Bạn có thể đặt mật khẩu mới.",
                icon    = Icons.Default.CheckCircle,
                type    = InfoBoxType.SUCCESS
            )

            Spacer(Modifier.height(20.dp))

            // Mật khẩu mới
            var showNew by remember { mutableStateOf(false) }
            val newIsError = state.error != null && !state.error.contains("xác nhận")

            Text("Mật khẩu mới",
                style = MaterialTheme.typography.labelMedium,
                color = if (newIsError) RedColor else TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value           = state.newPassword,
                onValueChange   = vm::onNewPasswordChange,
                modifier        = Modifier.fillMaxWidth(),
                placeholder     = { Text("Nhập mật khẩu mới", color = TextTertiary) },
                leadingIcon     = { Icon(Icons.Default.Lock, null, tint = if (newIsError) RedColor else TextTertiary) },
                trailingIcon    = {
                    IconButton(onClick = vm::onToggleNewPassword) {
                        Icon(
                            if (state.showNewPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            null, tint = TextTertiary
                        )
                    }
                },
                visualTransformation = if (state.showNewPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                singleLine      = true,
                isError         = newIsError,
                supportingText  = if (newIsError) { { Text(state.error!!, color = RedColor, fontSize = 12.sp) } } else null,
                shape           = RoundedCornerShape(12.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor,
                    errorBorderColor = RedColor, errorLeadingIconColor = RedColor
                )
            )

            // Strength bar — nhất quán với ChangePasswordScreen
            if (!newIsError) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthBar(strength = state.passwordStrength)
            }

            Spacer(Modifier.height(14.dp))

            // Xác nhận mật khẩu
            var showConfirm by remember { mutableStateOf(false) }
            val confirmIsError = state.error?.contains("xác nhận") == true

            Text("Xác nhận mật khẩu mới",
                style = MaterialTheme.typography.labelMedium,
                color = if (confirmIsError) RedColor else TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value           = state.confirmPassword,
                onValueChange   = vm::onConfirmPasswordChange,
                modifier        = Modifier.fillMaxWidth(),
                placeholder     = { Text("Nhập lại mật khẩu...", color = TextTertiary) },
                leadingIcon     = { Icon(Icons.Default.LockOpen, null, tint = if (confirmIsError) RedColor else TextTertiary) },
                trailingIcon    = {
                    IconButton(onClick = vm::onToggleConfirmPassword) {
                        Icon(
                            if (state.showConfirmPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            null, tint = TextTertiary
                        )
                    }
                },
                visualTransformation = if (state.showConfirmPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                singleLine      = true,
                isError         = confirmIsError,
                supportingText  = if (confirmIsError) { { Text(state.error!!, color = RedColor, fontSize = 12.sp) } } else null,
                shape           = RoundedCornerShape(12.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor,
                    errorBorderColor = RedColor, errorLeadingIconColor = RedColor
                )
            )

            Spacer(Modifier.height(14.dp))

            // Password rules — nhất quán với ChangePasswordScreen
            PasswordRules(password = state.newPassword)

            Spacer(Modifier.height(14.dp))

            InfoBox(
                message = "Mã PIN khẩn cấp vẫn được giữ nguyên. Bạn có thể đổi PIN trong Cài đặt bất cứ lúc nào.",
                icon    = Icons.Default.Info,
                type    = InfoBoxType.INFO
            )

            Spacer(Modifier.height(20.dp))

            LoadingButton(
                text           = "Xác nhận & Đăng nhập",
                loading        = state.isLoading,
                onClick        = vm::doResetPassword,
                modifier       = Modifier.fillMaxWidth(),
                containerColor = GreenPrimary
            )
        }
    }
}