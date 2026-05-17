package com.example.baotri.ui.ktv.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Device
import com.example.baotri.domain.model.MaintenanceLog
import com.example.baotri.domain.model.MaintenanceStatus
import com.example.baotri.domain.repository.DeviceRepository
import com.example.baotri.domain.usecase.log.GetDeviceLogsUseCase
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceDetailUiState(
    val device: Device? = null,
    val logs: List<MaintenanceLog> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepo: DeviceRepository,
    private val getDeviceLogs: GetDeviceLogsUseCase
) : ViewModel() {

    private val deviceId: Long = checkNotNull(savedStateHandle["deviceId"])
    private val _state = MutableStateFlow(DeviceDetailUiState())
    val state: StateFlow<DeviceDetailUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val device = deviceRepo.getDeviceById(deviceId)
        getDeviceLogs(deviceId)
            .collect { logs ->
                _state.value = DeviceDetailUiState(device = device, logs = logs, isLoading = false)
            }
    }
}

// ── Screen ────────────────────────────────────────────────────

@Composable
fun DeviceDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWriteLog: (Long) -> Unit,
    vm: DeviceDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val device = state.device

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Chi tiết thiết bị",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Button(
                    onClick = { device?.let { onNavigateToWriteLog(it.id) } },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddTask, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ghi maintenance log", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    ) { padding ->
        if (device == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Hero Card ──────────────────────────────────
            item {
                // Hero image/icon
                Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                        .background(
                            brush = linearGradient(
                                listOf(GreenPrimary, Color(0xFF1A9B7B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, null,
                        tint = androidx.compose.ui.graphics.Color.White.copy(.85f),
                        modifier = Modifier.size(52.dp))
                }

                // Info card
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(device.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mã: ${device.code} · ${device.category}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Spacer(Modifier.height(12.dp))

                    // 2x2 info grid
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCell("Vị trí", device.location, modifier = Modifier.weight(1f))
                        InfoCell("Ngày mua", device.buyDate?.let { DateUtil.format(it) } ?: "—",
                            modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCell(
                            label = "Hạn bảo hành",
                            value = device.warrantyDate?.let { DateUtil.format(it) } ?: "—",
                            valueColor = if (device.isWarrantyExpired) AmberColor else null,
                            modifier = Modifier.weight(1f)
                        )
                        InfoCell("Danh mục", device.category, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = device.latestStatus ?: MaintenanceStatus.RESOLVED)
                        if (device.isWarrantyExpired) {
                            Text("⚠ Hết bảo hành", fontSize = 12.sp, color = AmberColor, fontWeight = FontWeight.Medium)
                        } else if (device.isWarrantyExpiringSoon) {
                            Text("⚠ Sắp hết bảo hành", fontSize = 12.sp, color = AmberColor)
                        }
                    }
                }
                HorizontalDivider(color = BorderColor)
            }

            // ── Maintenance History ────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Lịch sử bảo trì")
                Spacer(Modifier.height(8.dp))
            }

            if (state.logs.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.Assignment,
                        message = "Chưa có lịch sử bảo trì")
                }
            } else {
                items(state.logs) { log ->
                    LogTimelineItem(log = log, isLast = log == state.logs.last())
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun LogTimelineItem(log: MaintenanceLog, isLast: Boolean) {
    val dotColor = when (log.status) {
        MaintenanceStatus.RESOLVED      -> GreenPrimary
        MaintenanceStatus.WAITING_PARTS -> AmberColor
        MaintenanceStatus.UNRESOLVED    -> RedColor
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Timeline dot + line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            if (!isLast) Box(modifier = Modifier.width(1.5.dp).weight(1f).background(BorderColor))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(log.description.take(60) + if (log.description.length > 60) "..." else "",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (log.solution.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(log.solution.take(80), fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("${log.performedByName} · ${DateUtil.formatDateTime(log.performedAt)}",
                fontSize = 11.sp, color = TextTertiary)
        }
    }
}
