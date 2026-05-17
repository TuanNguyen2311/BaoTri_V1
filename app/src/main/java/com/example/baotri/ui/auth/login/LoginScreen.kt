package com.example.baotri.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.domain.model.Role
import com.example.baotri.ui.shared.components.LoadingButton
import com.example.baotri.ui.shared.theme.*

@Composable
fun LoginScreen(
    onNavigateToChangePassword: (Long) -> Unit,
    onNavigateToKtvDashboard: (Long) -> Unit,
    onNavigateToManagerDashboard: (Long) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }

    // Handle navigation
    LaunchedEffect(state.navigateTo) {
        when (val nav = state.navigateTo) {
            is LoginNavEvent.ToChangePassword   -> { onNavigateToChangePassword(nav.userId); vm.clearNavEvent() }
            is LoginNavEvent.ToKtvDashboard     -> { onNavigateToKtvDashboard(nav.userId); vm.clearNavEvent() }
            is LoginNavEvent.ToManagerDashboard -> { onNavigateToManagerDashboard(nav.userId); vm.clearNavEvent() }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(40.dp))

        // ── Logo ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GreenLight)
                .border(1.dp, GreenPrimary.copy(.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Build, contentDescription = null,
                tint = GreenPrimary, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Bảo trì thiết bị",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("Hệ thống quản lý bảo trì",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary)

        Spacer(Modifier.height(36.dp))

        // ── Username ─────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Tên đăng nhập", style = MaterialTheme.typography.labelMedium,
                color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập tên đăng nhập", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = TextTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Password ──────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Mật khẩu", style = MaterialTheme.typography.labelMedium,
                color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập mật khẩu", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextTertiary) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = TextTertiary)
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { vm.login() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Role Selector ─────────────────────────────────────
        HorizontalDivider(color = BorderColor)
        Spacer(Modifier.height(4.dp))
        Text("Đăng nhập với tư cách",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoleChip(
                label = "Kỹ thuật viên",
                icon = Icons.Default.Engineering,
                selected = state.selectedRole == Role.TECHNICIAN,
                onClick = { vm.onRoleChange(Role.TECHNICIAN) },
                modifier = Modifier.weight(1f)
            )
            RoleChip(
                label = "Quản lý",
                icon = Icons.Default.AdminPanelSettings,
                selected = state.selectedRole == Role.MANAGER,
                onClick = { vm.onRoleChange(Role.MANAGER) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Error ─────────────────────────────────────────────
        if (state.error != null) {
            Text(state.error!!, color = RedColor,
                fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        }

        // ── Login Button ──────────────────────────────────────
        LoadingButton(
            text = "Đăng nhập",
            loading = state.isLoading,
            onClick = vm::login,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // ── Forgot Password ───────────────────────────────────
        TextButton(onClick = onNavigateToForgotPassword) {
            Text("Quên mật khẩu? Dùng mã PIN khẩn cấp",
                color = GreenPrimary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text("v1.0.0 · Offline mode",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary)
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) GreenLight else MaterialTheme.colorScheme.surfaceVariant
    val border = if (selected) GreenPrimary else BorderColor
    val tint   = if (selected) GreenPrimary else TextSecondary

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// Fix missing ImageVector import
private typealias ImageVector = androidx.compose.ui.graphics.vector.ImageVector
