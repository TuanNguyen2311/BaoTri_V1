package com.example.baotri.ui.manager.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Role
import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.UserRepository
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.auth.LogoutUseCase
import com.example.baotri.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.toReadableSize

data class SettingsUiState(
    val user: User? = null,
    val darkMode: Boolean = false,
    val storageUsedBytes: Long = 0L,
    val isLoading: Boolean = false
)

private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepo: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = getCurrentSession().userId
            val user = userRepo.getUserById(userId)
            val dbFile = context.getDatabasePath("baotri_db")
            val storageUsed = dbFile.length()
            _state.update { it.copy(user = user, storageUsedBytes = storageUsed) }
        }
        viewModelScope.launch {
            context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }
                .collect { dark -> _state.update { it.copy(darkMode = dark) } }
        }
    }

    fun toggleDarkMode() = viewModelScope.launch {
        val newVal = !_state.value.darkMode
        context.dataStore.edit { it[DARK_MODE_KEY] = newVal }
    }

    fun logout() = viewModelScope.launch { logoutUseCase() }
}

// ── Screen ─────────────────────────────────────────────────────


@Composable
fun SettingsScreen(
    isManager: Boolean,
    onLogout: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BaoTriTopBar(title = "Cài đặt") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile card
            item {
                val roleLabel = if (state.user?.role == Role.MANAGER) "Quản lý" else "Kỹ thuật viên"
                val roleColor = if (state.user?.role == Role.MANAGER) PurplePrimary else GreenPrimary
                val roleBg   = if (state.user?.role == Role.MANAGER) PurpleLight else GreenLight
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(50.dp).clip(CircleShape)
                                .background(roleBg).border(1.dp, roleColor.copy(.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isManager) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                null, tint = roleColor, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.user?.fullName ?: "...",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier.clip(CircleShape).background(roleBg)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(roleLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = roleColor)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
                    }
                }
            }

            // Tài khoản group
            item {
                SettingGroupLabel("Tài khoản")
                SettingCard {
                    SettingRow(icon = Icons.Default.Lock, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF1D4ED8),
                        label = "Đổi mật khẩu", onClick = {})
                    SettingDivider()
                    SettingRow(icon = Icons.Default.Badge, iconBg = GreenLight, iconTint = GreenPrimary,
                        label = "Thông tin cá nhân", onClick = {})
                    if (isManager) {
                        SettingDivider()
                        SettingRow(icon = Icons.Default.Pin, iconBg = PurpleLight, iconTint = PurplePrimary,
                            label = "Đổi PIN khẩn cấp", onClick = {})
                    }
                }
            }

            // Ứng dụng group
            item {
                SettingGroupLabel("Ứng dụng")
                SettingCard {
                    SettingRowToggle(
                        icon = Icons.Default.DarkMode, iconBg = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = TextSecondary, label = "Giao diện tối",
                        checked = state.darkMode, onToggle = { vm.toggleDarkMode() }
                    )
                    SettingDivider()
                    SettingRow(icon = Icons.Default.Language, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF1D4ED8),
                        label = "Ngôn ngữ", trailing = "Tiếng Việt", onClick = {})
                }
            }

            // Dữ liệu group (Manager only)
            if (isManager) {
                item {
                    SettingGroupLabel("Dữ liệu")
                    SettingCard {
                        // Storage bar
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storage, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                    Text("Bộ nhớ đã dùng", fontSize = 14.sp)
                                }
                                Text(state.storageUsedBytes.toReadableSize(), fontSize = 12.sp, color = TextSecondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (state.storageUsedBytes.toFloat() / (50 * 1024 * 1024)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)),
                                color = GreenPrimary, trackColor = BorderColor
                            )
                        }
                        SettingDivider()
                        SettingRow(icon = Icons.Default.Group, iconBg = PurpleLight, iconTint = PurplePrimary,
                            label = "Quản lý tài khoản KTV", onClick = onNavigateToAccounts)
                        SettingDivider()
                        SettingRow(icon = Icons.Default.CloudSync, iconBg = GreenLight, iconTint = GreenPrimary,
                            label = "Backup & Khôi phục", onClick = onNavigateToBackup)
                        SettingDivider()
                        SettingRow(icon = Icons.Default.DeleteForever, iconBg = RedLight, iconTint = RedColor,
                            label = "Xóa toàn bộ dữ liệu", labelColor = RedColor, onClick = {})
                    }
                }
            }

            // Về ứng dụng
            item {
                SettingGroupLabel("Về ứng dụng")
                SettingCard {
                    SettingRow(icon = Icons.Default.Info, iconBg = PurpleLight, iconTint = PurplePrimary,
                        label = "Phiên bản", trailing = "v1.0.0", onClick = {})
                    SettingDivider()
                    SettingRow(icon = Icons.Default.MenuBook, iconBg = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = TextSecondary, label = "Hướng dẫn sử dụng", onClick = {})
                }
            }

            // Logout
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = RedColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đăng xuất", color = RedColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Logout confirm dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc muốn đăng xuất? Mọi dữ liệu đã lưu vẫn được giữ nguyên.") },
            confirmButton = {
                Button(
                    onClick = { vm.logout(); onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Reusable setting widgets ──────────────────────────────────
@Composable
private fun SettingGroupLabel(label: String) {
    Text(label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary, letterSpacing = 0.6.sp,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { Column(content = content) }
}

@Composable
private fun SettingRow(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    label: String, trailing: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(label, fontSize = 14.sp, color = labelColor, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 13.sp, color = TextSecondary)
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingRowToggle(
    icon: ImageVector, iconBg: Color, iconTint: Color,
    label: String, checked: Boolean, onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary)
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = BorderColor, thickness = 0.5.dp)
}
