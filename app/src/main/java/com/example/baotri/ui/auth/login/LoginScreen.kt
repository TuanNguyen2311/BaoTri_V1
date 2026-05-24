package com.example.baotri.ui.auth.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.R
import com.example.baotri.domain.model.Role
import com.example.baotri.ui.shared.components.LoadingButton
import com.example.baotri.ui.shared.theme.*

@Composable
fun LoginScreen(
    onNavigateToChangePassword: (Long, Role) -> Unit,
    onNavigateToKtvDashboard: () -> Unit,
    onNavigateToManagerDashboard: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── One-shot navigation qua Channel ──────────────────────
    // Dùng LaunchedEffect với Unit key — chỉ collect 1 lần, không re-trigger
    LaunchedEffect(Unit) {
        vm.navEvents.collect { event ->
            when (event) {
                is LoginNavEvent.ToChangePassword  -> onNavigateToChangePassword(event.userId, event.role)
                is LoginNavEvent.ToKtvDashboard    -> onNavigateToKtvDashboard()
                is LoginNavEvent.ToManagerDashboard -> onNavigateToManagerDashboard()
            }
        }
    }

    // ── Hiển thị lỗi chung qua Snackbar ──────────────────────
    LaunchedEffect(state.generalError) {
        state.generalError?.let { error ->
            snackbarHostState.showSnackbar(
                message     = error,
                duration    = SnackbarDuration.Short,
                withDismissAction = true
            )
            vm.clearGeneralError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AmberColor,
                    contentColor   = androidx.compose.ui.graphics.Color.White,
                    shape          = RoundedCornerShape(10.dp)
                )
            }
        }
    ) { scaffoldPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                        // imePadding để content không bị keyboard che
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Flexible top space — co lại trên màn hình nhỏ
                    Spacer(Modifier.weight(0.8f))

                    // ── Logo ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenLight)
                            .border(1.dp, GreenPrimary.copy(.3f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Build, contentDescription = null,
                            tint = GreenPrimary, modifier = Modifier.size(36.dp)
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

                    // ── Username field ────────────────────────────────
                    LoginTextField(
                        value          = state.username,
                        onValueChange  = vm::onUsernameChange,
                        label          = stringResource(R.string.login_username_label),
                        placeholder    = stringResource(R.string.login_username_placeholder),
                        leadingIcon    = Icons.Default.Person,
                        error          = state.usernameError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction    = ImeAction.Next,
                            capitalization = KeyboardCapitalization.None
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Password field ────────────────────────────────
                    LoginTextField(
                        value          = state.password,
                        onValueChange  = vm::onPasswordChange,
                        label          = stringResource(R.string.login_password_label),
                        placeholder    = stringResource(R.string.login_password_placeholder),
                        leadingIcon    = Icons.Default.Lock,
                        error          = state.passwordError,
                        isPassword     = true,
                        showPassword   = state.showPassword,
                        onTogglePassword = vm::onTogglePassword,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            vm.login()
                        })
                    )

                    Spacer(Modifier.height(10.dp))

                    // ── Remember me ───────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.rememberMe,
                            onCheckedChange = vm::onRememberMeChange,
                            colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.login_remember_me),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Login button ──────────────────────────────────
                    LoadingButton(
                        text     = stringResource(R.string.login_button),
                        loading  = state.isLoading,
                        enabled = state.isFormValid,
                        onClick = {
                            focusManager.clearFocus()
                            vm.login()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Forgot password ───────────────────────────────
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text(
                            stringResource(R.string.login_forgot_password),
                            color = GreenPrimary, fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.weight(0.5f))


                }

                Text(
                    stringResource(R.string.app_version),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
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
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) RedColor else TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value          = value,
            onValueChange  = onValueChange,
            modifier       = Modifier.fillMaxWidth(),
            placeholder    = { Text(placeholder, color = TextTertiary) },
            leadingIcon    = {
                Icon(
                    leadingIcon, null,
                    tint = if (isError) RedColor else TextTertiary
                )
            },
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
            supportingText = if (isError) ({
                // Field-level error hiển thị ngay dưới input
                Text(error!!, color = RedColor, fontSize = 12.sp)
            }) else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape          = RoundedCornerShape(12.dp),
            colors         = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenPrimary,
                unfocusedBorderColor = BorderColor,
                errorBorderColor     = RedColor,
                errorLeadingIconColor = RedColor
            )
        )
    }
}
