package com.example.baotri.ui.ktv.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.*
import com.example.baotri.domain.repository.DeviceRepository
import com.example.baotri.domain.usecase.log.*
import com.example.baotri.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Screen + Bottom Sheet ─────────────────────────────────────
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.baotri.domain.model.MaintenanceStatus
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil

// ── ViewModel ─────────────────────────────────────────────────
data class DeviceDetailUiState(
    val device: Device? = null,
    val logs: List<MaintenanceLog> = emptyList(),
    val isLoading: Boolean = true
)

data class LogDetailSheetState(
    val isVisible: Boolean = false,
    val log: MaintenanceLog? = null,
    val isLoadingHistory: Boolean = false,
    // Cập nhật trạng thái
    val showUpdateForm: Boolean = false,
    val selectedStatus: MaintenanceStatus = MaintenanceStatus.RESOLVED,
    val updateNote: String = "",
    val updatePhotos: List<String> = emptyList(),
    val isUpdating: Boolean = false,
    val updateError: String? = null
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepo: DeviceRepository,
    private val getDeviceLogs: GetDeviceLogsUseCase,
    private val getLogWithHistory: GetLogWithHistoryUseCase,
    private val updateLogStatus: UpdateLogStatusUseCase,
    private val session: SessionManager
) : ViewModel() {

    private val deviceId: Long = checkNotNull(savedStateHandle["deviceId"])

    private val _state = MutableStateFlow(DeviceDetailUiState())
    val state: StateFlow<DeviceDetailUiState> = _state.asStateFlow()

    private val _sheetState = MutableStateFlow(LogDetailSheetState())
    val sheetState: StateFlow<LogDetailSheetState> = _sheetState.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val device = deviceRepo.getDeviceById(deviceId)
        getDeviceLogs(deviceId).collect { logs ->
            _state.value = DeviceDetailUiState(device = device, logs = logs, isLoading = false)
        }
    }

    // Mở bottom sheet — load log kèm history
    fun openLogDetail(logId: Long) = viewModelScope.launch {
        _sheetState.update { it.copy(isVisible = true, isLoadingHistory = true, showUpdateForm = false) }
        val logWithHistory = getLogWithHistory(logId)
        _sheetState.update { it.copy(log = logWithHistory, isLoadingHistory = false) }
    }

    fun closeSheet() {
        _sheetState.value = LogDetailSheetState()
    }

    // Toggle form cập nhật trạng thái
    fun showUpdateForm() = _sheetState.update {
        it.copy(
            showUpdateForm = true,
            selectedStatus = MaintenanceStatus.RESOLVED, // mặc định chọn RESOLVED
            updateNote = "",
            updatePhotos = emptyList(),
            updateError = null
        )
    }
    fun hideUpdateForm() = _sheetState.update { it.copy(showUpdateForm = false, updateError = null) }

    fun onUpdateStatusChange(s: MaintenanceStatus) = _sheetState.update { it.copy(selectedStatus = s) }
    fun onUpdateNoteChange(v: String) = _sheetState.update { it.copy(updateNote = v, updateError = null) }
    fun onUpdatePhotoAdded(uri: String) = _sheetState.update { it.copy(updatePhotos = it.updatePhotos + uri) }
    fun onUpdatePhotoRemoved(uri: String) = _sheetState.update { it.copy(updatePhotos = it.updatePhotos - uri) }

    fun submitStatusUpdate() = viewModelScope.launch {
        val sheet = _sheetState.value
        val logId = sheet.log?.id ?: return@launch
        _sheetState.update { it.copy(isUpdating = true, updateError = null) }

        val userId   = session.currentUserId.first()
        val fullName = session.currentFullName.first()

        updateLogStatus(
            logId            = logId,
            newStatus        = sheet.selectedStatus,
            changedByUserId  = userId,
            changedByName    = fullName,
            note             = sheet.updateNote,
            photoPaths       = sheet.updatePhotos
        ).onSuccess {
            // Reload log với history mới
            val updated = getLogWithHistory(logId)
            _sheetState.update { it.copy(isUpdating = false, showUpdateForm = false, log = updated) }
        }.onFailure { e ->
            _sheetState.update { it.copy(isUpdating = false, updateError = e.message) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWriteLog: (Long) -> Unit,
    vm: DeviceDetailViewModel = hiltViewModel()
) {
    val state     by vm.state.collectAsState()
    val sheetState by vm.sheetState.collectAsState()
    val device = state.device
    val sheetScaffoldState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show/hide bottom sheet
    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) sheetScaffoldState.show()
        else sheetScaffoldState.hide()
    }

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Chi tiết thiết bị",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
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
            // ── Hero + Info ────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp).background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(GreenPrimary, Color(0xFF1A9B7B))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (device.photoPath != null) {
                        AsyncImage(model = device.photoPath, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Settings, null,
                            tint = Color.White.copy(.85f), modifier = Modifier.size(52.dp))
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(device.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mã: ${device.code} · ${device.category}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCell("Vị trí", device.location, modifier = Modifier.weight(1f))
                        InfoCell("Ngày mua", device.buyDate?.let { DateUtil.format(it) } ?: "—", modifier = Modifier.weight(1f))
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
                        if (device.isWarrantyExpired)
                            Text("⚠ Hết bảo hành", fontSize = 12.sp, color = AmberColor, fontWeight = FontWeight.Medium)
                        else if (device.isWarrantyExpiringSoon)
                            Text("⚠ Sắp hết bảo hành", fontSize = 12.sp, color = AmberColor)
                    }
                }
                HorizontalDivider(color = BorderColor)
            }

            // ── Log list ───────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Lịch sử bảo trì")
                Spacer(Modifier.height(8.dp))
            }

            if (state.logs.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.Assignment, message = "Chưa có lịch sử bảo trì")
                }
            } else {
                items(state.logs) { log ->
                    LogSummaryItem(
                        log     = log,
                        onClick = { vm.openLogDetail(log.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Bottom Sheet chi tiết log ──────────────────────────
    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = vm::closeSheet,
            sheetState = sheetScaffoldState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LogDetailBottomSheet(
                state  = sheetState,
                onClose = vm::closeSheet,
                onShowUpdateForm = vm::showUpdateForm,
                onHideUpdateForm = vm::hideUpdateForm,
                onStatusChange   = vm::onUpdateStatusChange,
                onNoteChange     = vm::onUpdateNoteChange,
                onPhotoAdded     = vm::onUpdatePhotoAdded,
                onPhotoRemoved   = vm::onUpdatePhotoRemoved,
                onSubmit         = vm::submitStatusUpdate
            )
        }
    }
}

// ── Log Summary Item (danh sách) ──────────────────────────────
@Composable
private fun LogSummaryItem(log: com.example.baotri.domain.model.MaintenanceLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline dot
        val dotColor = when (log.status) {
            MaintenanceStatus.RESOLVED      -> GreenPrimary
            MaintenanceStatus.WAITING_PARTS -> AmberColor
            MaintenanceStatus.UNRESOLVED    -> RedColor
        }
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                log.description.take(60) + if (log.description.length > 60) "..." else "",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
            )
            Text("${log.performedByName} · ${DateUtil.formatDateTime(log.performedAt)}",
                fontSize = 11.sp, color = TextTertiary)
        }
        StatusBadge(status = log.status)
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderColor, thickness = 0.5.dp)
}

// ── Bottom Sheet nội dung ─────────────────────────────────────
@Composable
private fun LogDetailBottomSheet(
    state: LogDetailSheetState,
    onClose: () -> Unit,
    onShowUpdateForm: () -> Unit,
    onHideUpdateForm: () -> Unit,
    onStatusChange: (MaintenanceStatus) -> Unit,
    onNoteChange: (String) -> Unit,
    onPhotoAdded: (String) -> Unit,
    onPhotoRemoved: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val log = state.log
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onPhotoAdded(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chi tiết bảo trì", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
        }
        HorizontalDivider(color = BorderColor)

        if (state.isLoadingHistory || log == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Column
        }

        // ── Thông tin log ban đầu ──────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            DetailRow("Loại bảo trì", log.logType.displayName())
            Spacer(Modifier.height(10.dp))
            DetailRow("Mô tả sự cố", log.description)
            Spacer(Modifier.height(10.dp))
            DetailRow("Cách xử lý", log.solution.ifBlank { "Chưa có" })
            if (!log.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                DetailRow("Ghi chú", log.notes)
            }
        }

        // ── Ảnh đính kèm ban đầu ─────────────────────────
        if (log.photoPaths.isNotEmpty()) {
            HorizontalDivider(color = BorderColor)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Ảnh đính kèm", style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                PhotoGrid(photoPaths = log.photoPaths)
            }
        }

        // ── Timeline trạng thái ───────────────────────────
        HorizontalDivider(color = BorderColor)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Lịch sử trạng thái".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(bottom = 12.dp))

            log.statusHistory.forEachIndexed { idx, history ->
                val isLast = idx == log.statusHistory.lastIndex
                StatusTimelineItem(history = history, isLast = isLast)
            }
        }

        // ── Form cập nhật trạng thái ──────────────────────
        if (log.canUpdateStatus) {
            HorizontalDivider(color = BorderColor)
            if (!state.showUpdateForm) {
                // Nút mở form
                Button(
                    onClick = onShowUpdateForm,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cập nhật trạng thái", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Form cập nhật
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Cập nhật trạng thái".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(12.dp))

                    // Status chips — chỉ cho chọn RESOLVED hoặc UNRESOLVED (không cho chọn lại WAITING)
                    Text("Trạng thái mới *", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(MaintenanceStatus.RESOLVED, MaintenanceStatus.UNRESOLVED).forEach { s ->
                            val selected = state.selectedStatus == s
                            val (bg, fg) = when (s) {
                                MaintenanceStatus.RESOLVED  -> Pair(GreenLight, GreenPrimary)
                                else                        -> Pair(RedLight, RedColor)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (selected) bg else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (selected) fg else BorderColor, RoundedCornerShape(99.dp))
                                    .clickable { onStatusChange(s) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(s.displayName(), fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) fg else TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Ghi chú
                    Text("Ghi chú", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = state.updateNote,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        placeholder = { Text("VD: Đã lắp linh kiện mới, test hoạt động tốt", color = TextTertiary) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Ảnh đính kèm cho bước này
                    Text("Ảnh đính kèm (tùy chọn)", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.updatePhotos.forEach { uri ->
                            Box(modifier = Modifier.size(64.dp)) {
                                AsyncImage(model = uri, contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                                IconButton(
                                    onClick = { onPhotoRemoved(uri) },
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        if (state.updatePhotos.size < 5) {
                            Box(
                                modifier = Modifier.size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { photoLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                                    Text("Thêm", fontSize = 10.sp, color = TextTertiary)
                                }
                            }
                        }
                    }

                    if (state.updateError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.updateError, color = RedColor, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onHideUpdateForm,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) { Text("Hủy", color = TextSecondary) }
                        LoadingButton(
                            text = "Lưu cập nhật",
                            loading = state.isUpdating,
                            onClick = onSubmit,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}

// ── Status Timeline Item ──────────────────────────────────────
@Composable
private fun StatusTimelineItem(history: com.example.baotri.domain.model.LogStatusHistory, isLast: Boolean) {
    val (dotColor, badgeBg, badgeFg) = when (history.status) {
        MaintenanceStatus.RESOLVED      -> Triple(GreenPrimary, GreenLight, GreenPrimary)
        MaintenanceStatus.WAITING_PARTS -> Triple(AmberColor, AmberLight, AmberColor)
        MaintenanceStatus.UNRESOLVED    -> Triple(RedColor, RedLight, RedColor)
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        // Dot + line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(if (history.photoPaths.isNotEmpty()) 100.dp else 60.dp)
                    .background(BorderColor))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(history.status.displayName(), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = badgeFg)
                }
                Text(DateUtil.formatDateTime(history.changedAt), fontSize = 11.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(4.dp))
            Text("👤 ${history.changedByName}", fontSize = 12.sp, color = TextSecondary)
            if (history.note.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text("\"${history.note}\"", fontSize = 12.sp, color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            // Ảnh đính kèm riêng của bước này
            if (history.photoPaths.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PhotoGrid(photoPaths = history.photoPaths, size = 56)
            }
        }
    }
}

// ── Photo Grid ────────────────────────────────────────────────
@Composable
private fun PhotoGrid(photoPaths: List<String>, size: Int = 72) {
    var expandedUri by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photoPaths.forEach { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(size.dp).clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { expandedUri = uri },
                contentScale = ContentScale.Crop
            )
        }
    }
    // Full-screen preview khi tap ảnh
    if (expandedUri != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expandedUri = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { expandedUri = null }) {
                AsyncImage(
                    model = expandedUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────
@Composable
private fun InfoCell(
    label: String, value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        Text(value, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
