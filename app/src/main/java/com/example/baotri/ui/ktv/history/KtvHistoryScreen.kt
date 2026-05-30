package com.example.baotri.ui.ktv.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.MaintenanceLog
import com.example.baotri.domain.model.MaintenanceStatus
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.log.GetUserLogsUseCase
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KtvHistoryUiState(
    val logs: List<MaintenanceLog> = emptyList(),
    val filtered: List<MaintenanceLog> = emptyList(),
    val query: String = "",
    val statusFilter: MaintenanceStatus? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class KtvHistoryViewModel @Inject constructor(
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val getUserLogs: GetUserLogsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(KtvHistoryUiState())
    val state: StateFlow<KtvHistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = getCurrentSession().userId
            getUserLogs(userId).collect { logs ->
                val nonDraft = logs.filter { !it.isDraft }
                _state.update { s -> s.copy(logs = nonDraft, filtered = applyFilter(nonDraft, s.query, s.statusFilter), isLoading = false) }
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { s -> s.copy(query = q, filtered = applyFilter(s.logs, q, s.statusFilter)) }
    }

    fun onStatusFilter(status: MaintenanceStatus?) {
        _state.update { s -> s.copy(statusFilter = status, filtered = applyFilter(s.logs, s.query, status)) }
    }

    private fun applyFilter(logs: List<MaintenanceLog>, query: String, status: MaintenanceStatus?): List<MaintenanceLog> {
        return logs.filter { log ->
            val matchQ = query.isBlank() || log.deviceName.contains(query, true) || log.deviceCode.contains(query, true)
            val matchS = status == null || log.status == status
            matchQ && matchS
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────

@Composable
fun KtvHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevice: (Long) -> Unit,
    vm: KtvHistoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Lịch sử của tôi",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm tên hoặc mã thiết bị...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
            )
            // Status filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(label = "Tất cả", selected = state.statusFilter == null, onClick = { vm.onStatusFilter(null) })
                FilterChip(label = "Đã xử lý", selected = state.statusFilter == MaintenanceStatus.RESOLVED, onClick = { vm.onStatusFilter(MaintenanceStatus.RESOLVED) })
                FilterChip(label = "Chờ", selected = state.statusFilter == MaintenanceStatus.WAITING_PARTS, onClick = { vm.onStatusFilter(MaintenanceStatus.WAITING_PARTS) })
                FilterChip(label = "Chưa", selected = state.statusFilter == MaintenanceStatus.UNRESOLVED, onClick = { vm.onStatusFilter(MaintenanceStatus.UNRESOLVED) })
            }
            Spacer(Modifier.height(8.dp))
            if (state.filtered.isEmpty()) {
                EmptyState(icon = Icons.Default.History, message = "Không có log nào")
            } else {
                LazyColumn {
                    items(state.filtered) { log ->
                        LogHistoryItem(log = log, onClick = { onNavigateToDevice(log.deviceId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) GreenLight else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) GreenPrimary else BorderColor, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) GreenPrimary else TextSecondary)
    }
}

@Composable
private fun LogHistoryItem(log: MaintenanceLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Build, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(if (log.deviceName.isNotBlank()) log.deviceName else log.deviceCode,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(log.description.take(50) + if (log.description.length > 50) "..." else "",
                fontSize = 12.sp, color = TextSecondary)
            Text(DateUtil.formatDateTime(log.performedAt), fontSize = 11.sp, color = TextTertiary)
        }
        StatusBadge(status = log.status)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}
