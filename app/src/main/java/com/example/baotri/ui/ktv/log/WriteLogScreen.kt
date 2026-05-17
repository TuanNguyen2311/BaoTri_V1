package com.example.baotri.ui.ktv.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.baotri.domain.model.*
import com.example.baotri.domain.repository.DeviceRepository
import com.example.baotri.domain.usecase.log.SaveLogUseCase
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil
import com.example.baotri.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WriteLogUiState(
    val deviceName: String = "",
    val deviceCode: String = "",
    val deviceLocation: String = "",
    val logType: MaintenanceType = MaintenanceType.EMERGENCY,
    val description: String = "",
    val solution: String = "",
    val status: MaintenanceStatus = MaintenanceStatus.RESOLVED,
    val performedByName: String = "",
    val performedAt: Long = System.currentTimeMillis(),
    val photoPaths: List<String> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class WriteLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepo: DeviceRepository,
    private val saveLog: SaveLogUseCase,
    private val session: SessionManager
) : ViewModel() {

    private val deviceId: Long = checkNotNull(savedStateHandle["deviceId"])
    private val logId: Long = savedStateHandle["logId"] ?: 0L
    private var userId: Long = -1L

    private val _state = MutableStateFlow(WriteLogUiState())
    val state: StateFlow<WriteLogUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        userId = session.currentUserId.first()
        val fullName = session.currentFullName.first()
        val device = deviceRepo.getDeviceById(deviceId)
        _state.update { it.copy(
            deviceName     = device?.name ?: "",
            deviceCode     = device?.code ?: "",
            deviceLocation = device?.location ?: "",
            performedByName = fullName
        )}
    }

    fun onTypeChange(v: MaintenanceType)     = _state.update { it.copy(logType = v) }
    fun onDescriptionChange(v: String)       = _state.update { it.copy(description = v, error = null) }
    fun onSolutionChange(v: String)          = _state.update { it.copy(solution = v, error = null) }
    fun onStatusChange(v: MaintenanceStatus) = _state.update { it.copy(status = v) }
    fun onPerformedAtChange(v: Long)         = _state.update { it.copy(performedAt = v) }
    fun onNotesChange(v: String)             = _state.update { it.copy(notes = v) }
    fun onPhotoAdded(uri: String)            = _state.update { it.copy(photoPaths = it.photoPaths + uri) }
    fun onPhotoRemoved(uri: String)          = _state.update { it.copy(photoPaths = it.photoPaths - uri) }

    fun save(isDraft: Boolean = false) = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        val log = MaintenanceLog(
            id              = logId,
            deviceId        = deviceId,
            deviceName      = s.deviceName,
            deviceCode      = s.deviceCode,
            userId          = userId,
            performedByName = s.performedByName,
            logType         = s.logType,
            description     = s.description,
            solution        = s.solution,
            status          = s.status,
            photoPaths      = s.photoPaths,
            notes           = s.notes.ifBlank { null },
            isDraft         = isDraft,
            performedAt     = s.performedAt,
            createdAt       = System.currentTimeMillis()
        )
        saveLog(log)
            .onSuccess { _state.update { it.copy(isLoading = false, saved = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }
}

// ── Screen ────────────────────────────────────────────────────

@Composable
fun WriteLogScreen(
    onNavigateBack: () -> Unit,
    vm: WriteLogViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.onPhotoAdded(it.toString()) } }

    LaunchedEffect(state.saved) { if (state.saved) onNavigateBack() }

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Ghi maintenance log",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Device strip ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenLight)
                    .border(1.dp, GreenPrimary.copy(.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                Column {
                    Text(state.deviceName, fontWeight = FontWeight.SemiBold, color = GreenPrimary, fontSize = 13.sp)
                    Text("${state.deviceCode} · ${state.deviceLocation}", fontSize = 11.sp, color = Color(0xFF1A9B7B))
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // ── Log type ───────────────────────────────
                FormLabel("Loại bảo trì", required = true)
                DropdownField(
                    value = state.logType.displayName(),
                    options = MaintenanceType.values().map { it.displayName() },
                    onSelect = { idx -> vm.onTypeChange(MaintenanceType.values()[idx]) }
                )

                Spacer(Modifier.height(14.dp))

                // ── Description ────────────────────────────
                FormLabel("Mô tả sự cố / lỗi", required = true)
                OutlinedTextField(
                    value = state.description,
                    onValueChange = vm::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    placeholder = { Text("Mô tả triệu chứng lỗi...", color = TextTertiary) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
                )

                Spacer(Modifier.height(14.dp))

                // ── Solution ───────────────────────────────
                FormLabel("Cách xử lý", required = true)
                OutlinedTextField(
                    value = state.solution,
                    onValueChange = vm::onSolutionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    placeholder = { Text("Mô tả cách xử lý, linh kiện thay thế...", color = TextTertiary) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
                )

                Spacer(Modifier.height(14.dp))

                // ── Status chips ───────────────────────────
                FormLabel("Trạng thái sau xử lý", required = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaintenanceStatus.values().forEach { s ->
                        val selected = state.status == s
                        val (bg, fg, border) = when (s) {
                            MaintenanceStatus.RESOLVED      -> Triple(if (selected) GreenLight else Color.Transparent, GreenPrimary, GreenPrimary)
                            MaintenanceStatus.WAITING_PARTS -> Triple(if (selected) AmberLight else Color.Transparent, AmberColor, AmberColor)
                            MaintenanceStatus.UNRESOLVED    -> Triple(if (selected) RedLight else Color.Transparent, RedColor, RedColor)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (selected) bg else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (selected) border else BorderColor, RoundedCornerShape(99.dp))
                                .clickable { vm.onStatusChange(s) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(s.displayName(), fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) fg else TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Performed by ───────────────────────────
                FormLabel("Người thực hiện")
                OutlinedTextField(
                    value = state.performedByName,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = TextTertiary) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = BorderColor,
                        disabledTextColor = MaterialTheme.colorScheme.onBackground)
                )

                Spacer(Modifier.height(14.dp))

                // ── Date time ─────────────────────────────
                FormLabel("Thời gian thực hiện", required = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = DateUtil.format(state.performedAt),
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = TextTertiary) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = BorderColor,
                            disabledTextColor = MaterialTheme.colorScheme.onBackground)
                    )
                    OutlinedTextField(
                        value = DateUtil.formatTime(state.performedAt),
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = TextTertiary) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = BorderColor,
                            disabledTextColor = MaterialTheme.colorScheme.onBackground)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Photos ─────────────────────────────────
                FormLabel("Ảnh đính kèm")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.photoPaths.forEach { uri ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(model = uri, contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop)
                            IconButton(
                                onClick = { vm.onPhotoRemoved(uri) },
                                modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (state.photoPaths.size < 5) {
                        Box(
                            modifier = Modifier.size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { photoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = TextTertiary, modifier = Modifier.size(22.dp))
                                Text("Thêm", fontSize = 10.sp, color = TextTertiary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Notes ──────────────────────────────────
                FormLabel("Ghi chú thêm")
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = vm::onNotesChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    placeholder = { Text("Ghi chú khác (không bắt buộc)...", color = TextTertiary) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
                )

                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = RedColor, fontSize = 13.sp)
                }

                Spacer(Modifier.height(20.dp))

                LoadingButton(
                    text = "Lưu log bảo trì",
                    loading = state.isLoading,
                    onClick = { vm.save(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.save(true) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lưu nháp", color = TextSecondary)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FormLabel(text: String, required: Boolean = false) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        if (required) Text(" *", color = RedColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(value: String, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(idx); expanded = false })
            }
        }
    }
}
