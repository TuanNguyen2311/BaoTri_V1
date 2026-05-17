package com.example.baotri.ui.auth.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

// ═══════════════════════════════════════════════════════════
// CHANGE PASSWORD SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
fun ChangePasswordScreen(
    userId: Long,
    onNavigateToSetupPin: (Long) -> Unit,
    vm: ChangePasswordViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.navigateToSetupPin) {
        if (state.navigateToSetupPin) { onNavigateToSetupPin(userId); vm.clearNav() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // Step indicator
        StepIndicator(current = 2, total = 3)

        Spacer(Modifier.height(24.dp))

        // Warning box
        InfoBox(
            message = "Bạn đang dùng mật khẩu mặc định. Vui lòng đổi mật khẩu trước khi tiếp tục sử dụng.",
            icon = Icons.Default.Shield,
            type = InfoBoxType.WARNING
        )

        Spacer(Modifier.height(20.dp))

        Text("Tạo mật khẩu mới",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Mật khẩu dùng để đăng nhập tài khoản Quản lý",
            color = TextSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))

        // New password field
        Text("Mật khẩu mới", style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = state.newPassword,
            onValueChange = vm::onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Nhập mật khẩu mới", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextTertiary) },
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextTertiary)
                }
            },
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
        )

        // Strength bar
        Spacer(Modifier.height(8.dp))
        PasswordStrengthBar(strength = vm.passwordStrength)

        Spacer(Modifier.height(14.dp))

        // Confirm password
        Text("Xác nhận mật khẩu", style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = vm::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Nhập lại mật khẩu...", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = TextTertiary) },
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextTertiary)
                }
            },
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
        )

        Spacer(Modifier.height(16.dp))

        // Rules
        PasswordRules(password = state.newPassword)

        Spacer(Modifier.height(8.dp))

        if (state.error != null) {
            Text(state.error!!, color = RedColor, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        LoadingButton(
            text = "Xác nhận & Tiếp tục",
            loading = state.isLoading,
            onClick = { vm.confirm(userId) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════
// SETUP PIN SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
fun SetupPinScreen(
    userId: Long,
    onNavigateToPinReveal: (Long, String) -> Unit,
    vm: SetupPinViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.navigateToPinReveal) {
        if (state.navigateToPinReveal) {
            onNavigateToPinReveal(userId, vm.getEnteredPin())
            vm.clearNav()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        StepIndicator(current = 3, total = 3)
        Spacer(Modifier.height(24.dp))

        Text(
            if (state.step == PinStep.ENTER) "Tạo mã PIN khẩn cấp" else "Xác nhận mã PIN",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.step == PinStep.ENTER)
                "PIN 6 số dùng để reset mật khẩu nếu quên. Ghi lại và giữ ở nơi an toàn."
            else "Nhập lại PIN vừa tạo để xác nhận.",
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        InfoBox(
            message = "PIN này không thể khôi phục nếu mất. Nên ghi ra giấy và cất trong két hoặc tủ khóa.",
            icon = Icons.Default.Info,
            type = InfoBoxType.INFO
        )

        Spacer(Modifier.height(20.dp))

        Text("Nhập mã PIN 6 số", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(12.dp))

        // PIN dots
        val currentPin = if (state.step == PinStep.ENTER) state.pin else state.confirmPin
        PinDots(filled = currentPin.length, total = 6, color = PurplePrimary)

        Spacer(Modifier.height(8.dp))
        if (state.error != null) {
            Text(state.error!!, color = RedColor, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        // Numpad
        PinNumpad(onDigit = { d -> vm.onDigitEntered(d, userId) }, onBackspace = vm::onBackspace)
    }
}

// ═══════════════════════════════════════════════════════════
// PIN REVEAL SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
fun PinRevealScreen(
    pin: String,
    onNavigateToDashboard: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var confirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Success icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GreenLight)
                .border(2.dp, GreenPrimary.copy(.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, null, tint = GreenPrimary, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("PIN đã được lưu!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Ghi lại mã PIN bên dưới và cất ở nơi an toàn",
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))

        // PIN Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔐 Mã PIN khẩn cấp của bạn",
                        style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(pin)) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null,
                            tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sao chép", color = PurplePrimary, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // PIN digits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    pin.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(digit.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        InfoBox(
            message = "Màn hình này chỉ hiển thị MỘT LẦN DUY NHẤT. App sẽ không hiển thị lại PIN sau khi bấm Hoàn tất.",
            icon = Icons.Default.Warning,
            type = InfoBoxType.WARNING
        )

        Spacer(Modifier.height(20.dp))

        // Confirm checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { confirmed = !confirmed },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it },
                colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
            )
            Spacer(Modifier.width(8.dp))
            Text("Tôi đã ghi lại mã PIN và cất giữ cẩn thận", fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNavigateToDashboard,
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                disabledContainerColor = BorderColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hoàn tất thiết lập", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ── Shared PIN Components ─────────────────────────────────────
@Composable
fun PinDots(filled: Int, total: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) color else Color.Transparent)
                    .border(2.dp, if (i < filled) color else BorderColor, CircleShape)
            )
        }
    }
}

@Composable
fun PinNumpad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val keys = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.weight(1f).height(52.dp))
                        "⌫" -> OutlinedButton(
                            onClick = onBackspace,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                        ) { Text("⌫", fontSize = 22.sp, color = TextSecondary) }
                        else -> OutlinedButton(
                            onClick = { onDigit(key) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) { Text(key, fontSize = 22.sp, fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground) }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..total) {
            val done = i < current
            val active = i == current
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(when { done -> GreenPrimary; active -> PurplePrimary; else -> MaterialTheme.colorScheme.surfaceVariant })
                    .border(1.dp, if (!done && !active) BorderColor else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                else Text(i.toString(), color = if (active) Color.White else TextTertiary,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (i < total) {
                HorizontalDivider(
                    modifier = Modifier.width(32.dp),
                    color = if (done) GreenPrimary else BorderColor,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthBar(strength: Int) {
    val colors = listOf(RedColor, AmberColor, AmberColor, GreenPrimary)
    val label = listOf("Rất yếu", "Yếu", "Trung bình", "Mạnh")

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..4).forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= strength) colors[strength.coerceAtMost(3)] else BorderColor)
                )
            }
        }
        if (strength > 0) {
            Spacer(Modifier.height(4.dp))
            Text(label[strength.coerceAtMost(3)],
                fontSize = 11.sp, color = colors[strength.coerceAtMost(3)])
        }
    }
}

@Composable
fun PasswordRules(password: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        RuleRow("Ít nhất 6 ký tự", password.length >= 6)
        RuleRow("Có chữ hoa và chữ thường",
            password.any { it.isUpperCase() } && password.any { it.isLowerCase() })
        RuleRow("Có ít nhất 1 số", password.any { it.isDigit() })
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null, tint = if (met) GreenPrimary else TextTertiary, modifier = Modifier.size(16.dp)
        )
        Text(text, fontSize = 12.sp, color = if (met) GreenPrimary else TextTertiary)
    }
}
