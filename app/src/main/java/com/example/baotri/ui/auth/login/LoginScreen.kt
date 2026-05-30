package com.example.baotri.ui.auth.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.R
import com.example.baotri.domain.model.Role as AppRole
import com.example.baotri.ui.shared.components.InfoBox
import com.example.baotri.ui.shared.components.InfoBoxType
import com.example.baotri.ui.shared.components.LoadingButton
import com.example.baotri.ui.shared.theme.*

@Composable
fun LoginScreen(
    onNavigateToChangePassword: (Long, AppRole) -> Unit,
    onNavigateToKtvDashboard: () -> Unit,
    onNavigateToManagerDashboard: () -> Unit,
    onNavigateToForgotPassword: (username: String) -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current

    // Điều hướng sau khi login thành công
    LaunchedEffect(Unit) {
        vm.navEvents.collect { event ->
            when (event) {
                is LoginNavEvent.ToChangePassword   -> onNavigateToChangePassword(event.userId, event.role)
                is LoginNavEvent.ToKtvDashboard     -> onNavigateToKtvDashboard()
                is LoginNavEvent.ToManagerDashboard -> onNavigateToManagerDashboard()
            }
        }
    }

    // Khi error xuất hiện: đóng keyboard → imePadding thu lại → form full-screen → error hiện rõ
    LaunchedEffect(state.generalError) {
        if (state.generalError != null) {
            focusManager.clearFocus()
        }
    }

    Scaffold { scaffoldPadding ->
        LoginContent(
            scaffoldPadding            = scaffoldPadding,
            state                      = state,
            onTapOutside               = { focusManager.clearFocus() },
            onUserChange               = vm::onUsernameChange,
            onPasswordChange           = vm::onPasswordChange,
            onTogglePassword           = vm::onTogglePassword,
            onNext                     = { focusManager.moveFocus(FocusDirection.Down) },
            // Dismiss keyboard ngay khi submit — không chờ kết quả
            // → keyboard gone trước khi navigation transition bắt đầu → không lag
            onDone                     = { focusManager.clearFocus(); vm.login() },
            onRememberMeChange         = vm::onRememberMeChange,
            onNavigateToForgotPassword = { onNavigateToForgotPassword(state.username.trim()) }
        )
    }
}

@Composable
private fun LoginContent(
    scaffoldPadding: PaddingValues,
    state: LoginUiState,
    onTapOutside: () -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTapOutside() }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.8f))

                // ── Logo ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(Modifier.weight(0.6f))

                // ── Username ──────────────────────────────────
                LoginTextField(
                    value           = state.username,
                    onValueChange   = onUserChange,
                    label           = stringResource(R.string.login_username_label),
                    placeholder     = stringResource(R.string.login_username_placeholder),
                    leadingIcon     = Icons.Default.Person,
                    error           = state.usernameError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType   = KeyboardType.Text,
                        imeAction      = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(onNext = { onNext() })
                )

                Spacer(Modifier.height(12.dp))

                // ── Password ──────────────────────────────────
                LoginTextField(
                    value            = state.password,
                    onValueChange    = onPasswordChange,
                    label            = stringResource(R.string.login_password_label),
                    placeholder      = stringResource(R.string.login_password_placeholder),
                    leadingIcon      = Icons.Default.Lock,
                    error            = state.passwordError,
                    isPassword       = true,
                    showPassword     = state.showPassword,
                    onTogglePassword = onTogglePassword,
                    keyboardOptions  = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions  = KeyboardActions(onDone = { onDone() })
                )

                Spacer(Modifier.height(10.dp))

                // ── Remember Me ───────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Checkbox) { onRememberMeChange(!state.rememberMe) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked         = state.rememberMe,
                        onCheckedChange = null,
                        colors          = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.login_remember_me),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // ── Inline error — xuất hiện khi login thất bại ───────
                // Keyboard đã được đóng bởi LaunchedEffect ở LoginScreen,
                // nên imePadding thu lại và error này luôn nhìn thấy được
                AnimatedVisibility(
                    visible = state.generalError != null,
                    enter   = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit    = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        InfoBox(
                            message = state.generalError ?: "",
                            icon    = Icons.Default.ErrorOutline,
                            type    = InfoBoxType.ERROR
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Login button ──────────────────────────────
                LoadingButton(
                    text     = stringResource(R.string.login_button),
                    loading  = state.isLoading,
                    enabled  = state.isFormValid,
                    onClick  = { onDone() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // ── Forgot password ───────────────────────────
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text(
                        stringResource(R.string.login_forgot_password),
                        color    = GreenPrimary,
                        fontSize = 13.sp
                    )
                }

                AnimatedVisibility(visible = state.username.isBlank()) {
                    Text(
                        "Nhập tên đăng nhập để tự động điền vào bước tiếp theo",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(Modifier.weight(0.5f))
            }

            Text(
                stringResource(R.string.app_version),
                style    = MaterialTheme.typography.labelSmall,
                color    = TextTertiary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ── Reusable Login TextField ──────────────────────────────────
@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String? = null,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isError = error != null
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style    = MaterialTheme.typography.labelMedium,
            color    = if (isError) RedColor else TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value          = value,
            onValueChange  = onValueChange,
            modifier       = Modifier.fillMaxWidth(),
            placeholder    = { Text(placeholder, color = TextTertiary) },
            leadingIcon    = { Icon(leadingIcon, null, tint = if (isError) RedColor else TextTertiary) },
            trailingIcon   = if (isPassword) ({
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = TextTertiary
                    )
                }
            }) else null,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            singleLine     = true,
            isError        = isError,
            supportingText = if (isError) ({ Text(error!!, color = RedColor, fontSize = 12.sp) }) else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape          = RoundedCornerShape(12.dp),
            colors         = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = GreenPrimary,
                unfocusedBorderColor  = BorderColor,
                errorBorderColor      = RedColor,
                errorLeadingIconColor = RedColor
            )
        )
    }
}

// ── Previews ──────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true, name = "Login — no error")
@Composable
fun LoginScreenPreview() {
    Scaffold { p ->
        LoginContent(
            scaffoldPadding = p,
            state = LoginUiState(username = "admin", password = "Admin123", rememberMe = true),
            onTapOutside = {}, onUserChange = {}, onPasswordChange = {},
            onTogglePassword = {}, onNext = {}, onDone = {}, onRememberMeChange = {},
            onNavigateToForgotPassword = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login — with inline error")
@Composable
fun LoginScreenErrorPreview() {
    Scaffold { p ->
        LoginContent(
            scaffoldPadding = p,
            state = LoginUiState(
                username     = "admin",
                password     = "wrong",
                generalError = "Tên đăng nhập hoặc mật khẩu không đúng"
            ),
            onTapOutside = {}, onUserChange = {}, onPasswordChange = {},
            onTogglePassword = {}, onNext = {}, onDone = {}, onRememberMeChange = {},
            onNavigateToForgotPassword = {}
        )
    }
}