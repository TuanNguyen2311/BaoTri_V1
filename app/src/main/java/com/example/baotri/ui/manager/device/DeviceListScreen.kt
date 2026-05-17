package com.example.baotri.ui.manager.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Device
import com.example.baotri.domain.usecase.device.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
// ── Screen ─────────────────────────────────────────────────────
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.baotri.domain.model.MaintenanceStatus
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

data class DeviceListUiState(
    val devices: List<Device> = emptyList(),
    val query: String = "",
    val areaFilter: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val getAllDevices: GetAllDevicesUseCase,
    private val searchDevices: SearchDevicesUseCase,
    private val deleteDevice: DeleteDeviceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceListUiState())
    val state: StateFlow<DeviceListUiState> = _state.asStateFlow()
    private val _query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _query.debounce(300).flatMapLatest { q ->
                if (q.isBlank()) getAllDevices() else searchDevices(q)
            }.collect { list ->
                _state.update { it.copy(devices = list, isLoading = false) }
            }
        }
    }

    fun onQueryChange(q: String) { _query.value = q; _state.update { it.copy(query = q) } }

    fun deleteDevice(deviceId: Long) = viewModelScope.launch {
        deleteDevice.invoke(deviceId)
    }
}



@Composable
fun DeviceListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToEditDevice: (Long) -> Unit,
    onNavigateToDeviceDetail: (Long) -> Unit,
    vm: DeviceListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val areas = listOf("Tất cả", "Khu A", "Khu B", "Khu C")
    var selectedArea by remember { mutableStateOf("Tất cả") }

    val filtered = if (selectedArea == "Tất cả") state.devices
    else state.devices.filter { it.location.contains(selectedArea, true) }

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Thiết bị",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = onNavigateToAddDevice) {
                        Icon(Icons.Default.Add, null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm mới", color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm tên hoặc mã thiết bị...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary) },
                trailingIcon = { Icon(Icons.Default.Tune, null, tint = TextTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor)
            )
            // Area filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(areas) { area ->
                    val sel = selectedArea == area
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (sel) PurpleLight else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (sel) PurplePrimary else BorderColor, RoundedCornerShape(99.dp))
                            .clickable { selectedArea = area }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(area, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (sel) PurplePrimary else TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // Count
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} thiết bị", fontSize = 12.sp, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Sort, null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                    Text("Mới nhất", fontSize = 12.sp, color = PurplePrimary)
                }
            }
            if (filtered.isEmpty()) {
                EmptyState(icon = Icons.Default.Build, message = "Không có thiết bị nào")
            } else {
                LazyColumn {
                    items(filtered) { device ->
                        DeviceListItem(
                            device = device,
                            onClick = { onNavigateToDeviceDetail(device.id) },
                            onEdit = { onNavigateToEditDevice(device.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(device: Device, onClick: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${device.code} · ${device.location}", fontSize = 11.sp, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(status = device.latestStatus ?: MaintenanceStatus.RESOLVED)
            Text("${device.logCount} log", fontSize = 11.sp, color = TextTertiary)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}
