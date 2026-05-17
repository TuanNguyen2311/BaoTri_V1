package com.example.baotri.ui.manager.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.User
import com.example.baotri.domain.repository.UserRepository
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
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
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userRepo.getAllTechnicians().collect { list ->
                _state.update { it.copy(technicians = list) }
            }
        }
    }

    fun showAddDialog()  = _state.update { it.copy(showAddDialog = true, error = null) }
    fun dismissDialog()  = _state.update { it.copy(showAddDialog = false, newUsername = "", newFullName = "", newPassword = "", error = null) }
    fun onUsernameChange(v: String) = _state.update { it.copy(newUsername = v) }
    fun onFullNameChange(v: String) = _state.update { it.copy(newFullName = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(newPassword = v) }

    fun addTechnician() = viewModelScope.launch {
        val s = _state.value
        if (s.newFullName.isBlank()) { _state.update { it.copy(error = "Vui lòng nhập họ tên") }; return@launch }
        if (s.newUsername.isBlank()) { _state.update { it.copy(error = "Vui lòng nhập tên đăng nhập") }; return@launch }
        _state.update { it.copy(isLoading = true) }
        try {
            userRepo.createTechnician(s.newUsername, s.newPassword.ifBlank { "1234" }, s.newFullName)
            _state.update { it.copy(isLoading = false, showAddDialog = false, newUsername = "", newFullName = "", newPassword = "") }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun resetPassword(userId: Long) = viewModelScope.launch { userRepo.resetTechnicianPassword(userId) }
    fun toggleActive(userId: Long, active: Boolean) = viewModelScope.launch { userRepo.setTechnicianActive(userId, !active) }
}

// ── Screen ─────────────────────────────────────────────────────

@Composable
fun AccountManagementScreen(
    onNavigateBack: () -> Unit,
    vm: AccountManagementViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Quản lý tài khoản KTV",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = vm::showAddDialog) {
                        Icon(Icons.Default.PersonAdd, null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm KTV", color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.technicians.isEmpty()) {
                item { EmptyState(icon = Icons.Default.Group, message = "Chưa có kỹ thuật viên nào") }
            } else {
                items(state.technicians) { user ->
                    TechnicianItem(user = user,
                        onReset = { vm.resetPassword(user.id) },
                        onToggleActive = { vm.toggleActive(user.id, user.isActive) }
                    )
                }
            }
        }
    }

    // Add dialog
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissDialog,
            title = { Text("Thêm Kỹ thuật viên", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = state.newFullName, onValueChange = vm::onFullNameChange,
                        label = { Text("Họ và tên *") }, singleLine = true,
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor))
                    OutlinedTextField(value = state.newUsername, onValueChange = vm::onUsernameChange,
                        label = { Text("Tên đăng nhập *") }, singleLine = true,
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor))
                    OutlinedTextField(value = state.newPassword, onValueChange = vm::onPasswordChange,
                        label = { Text("Mật khẩu (mặc định: 1234)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor))
                    if (state.error != null) Text(state.error!!, color = RedColor, fontSize = 12.sp)
                }
            },
            confirmButton = {
                LoadingButton(text = "Tạo tài khoản", loading = state.isLoading,
                    onClick = vm::addTechnician, containerColor = PurplePrimary,
                    modifier = Modifier.height(40.dp))
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDialog) { Text("Hủy", color = TextSecondary) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TechnicianItem(user: User, onReset: () -> Unit, onToggleActive: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(if (user.isActive) GreenLight else MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, if (user.isActive) GreenPrimary.copy(.3f) else BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null,
                tint = if (user.isActive) GreenPrimary else TextTertiary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(user.username, fontSize = 12.sp, color = TextSecondary)
            if (!user.isActive) {
                Text("Đã vô hiệu hóa", fontSize = 11.sp, color = RedColor)
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = TextSecondary) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Reset mật khẩu về 1234") },
                    leadingIcon = { Icon(Icons.Default.LockReset, null) },
                    onClick = { onReset(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(if (user.isActive) "Vô hiệu hóa" else "Kích hoạt lại") },
                    leadingIcon = { Icon(if (user.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd, null) },
                    onClick = { onToggleActive(); showMenu = false }
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}
