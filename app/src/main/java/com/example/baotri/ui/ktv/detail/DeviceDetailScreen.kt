package com.example.baotri.ui.ktv.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWriteLog: (Long) -> Unit,
    vm: DeviceDetailViewModel = hiltViewModel()
) {
    val state      by vm.state.collectAsState()
    val sheetState by vm.sheetState.collectAsState()
    val device = state.device
    val sheetScaffoldState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = System.currentTimeMillis()

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
            if (state.isKtv && device != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(Modifier.navigationBarsPadding()) {
                        Button(
                            onClick = { onNavigateToWriteLog(device.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AddTask, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ghi maintenance log", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
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

            // ── Device info card ───────────────────────────────
            item {
                DeviceInfoCard(device = device, now = now)
            }

            // ── Log history section ────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Lịch sử bảo trì")
                Spacer(Modifier.height(8.dp))
            }

            if (state.logs.isEmpty()) {
                item {
                    EmptyState(icon = Icons.Default.Assignment, message = "Chưa có lịch sử bảo trì")
                }
            } else {
                items(state.logs) { log ->
                    LogSummaryItem(log = log, onClick = { vm.openLogDetail(log.id) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Bottom Sheet chi tiết log ──────────────────────────────
    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = vm::closeSheet,
            sheetState = sheetScaffoldState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LogDetailBottomSheet(
                state            = sheetState,
                onClose          = vm::closeSheet,
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

// ── Device Info Card ──────────────────────────────────────────
@Composable
private fun DeviceInfoCard(device: com.example.baotri.domain.model.Device, now: Long) {
    var showFullPhoto by remember { mutableStateOf(false) }
    val photoModel = device.photoPath?.let {
        if (it.startsWith("/")) java.io.File(it) else it
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Row: info bên trái + thumbnail bên phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Cột thông tin
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${device.code} · ${device.category}",
                        fontSize = 12.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp))

                    Spacer(Modifier.height(12.dp))

                    StatusBadge(status = device.latestStatus ?: MaintenanceStatus.RESOLVED)
                    when {
                        device.isWarrantyExpired(now) -> {
                            Spacer(Modifier.height(6.dp))
                            WarrantyChip("Hết bảo hành", AmberColor, AmberLight)
                        }
                        device.isWarrantyExpiringSoon(now) -> {
                            Spacer(Modifier.height(6.dp))
                            WarrantyChip("Sắp hết bảo hành", AmberColor, AmberLight)
                        }
                    }
                }

                // Thumbnail bên phải (chỉ hiện khi có ảnh)
                if (photoModel != null) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFullPhoto = true }
                    ) {
                        AsyncImage(
                            model = photoModel, contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ZoomIn, null,
                                tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor, thickness = 0.5.dp)

            // Thông tin chi tiết
            InfoIconRow(icon = Icons.Default.LocationOn,    label = "Vị trí",        value = device.location)
            Spacer(Modifier.height(10.dp))
            InfoIconRow(icon = Icons.Default.CalendarToday, label = "Ngày mua",
                value = device.buyDate?.let { DateUtil.format(it) } ?: "—")
            Spacer(Modifier.height(10.dp))
            InfoIconRow(
                icon       = Icons.Default.VerifiedUser,
                label      = "Hạn bảo hành",
                value      = device.warrantyDate?.let { DateUtil.format(it) } ?: "—",
                valueColor = if (device.isWarrantyExpired(now)) AmberColor else null
            )
        }
    }

    // Dialog xem ảnh full (đặt ngoài Card để không bị clip)
    if (showFullPhoto && photoModel != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullPhoto = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showFullPhoto = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoModel, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun InfoIconRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, fontSize = 11.sp, color = TextTertiary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = valueColor ?: MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun WarrantyChip(label: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ── Log Summary Item ──────────────────────────────────────────
@Composable
private fun LogSummaryItem(log: com.example.baotri.domain.model.MaintenanceLog, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        if (log.photoPaths.isNotEmpty()) {
            HorizontalDivider(color = BorderColor)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Ảnh đính kèm", style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                PhotoGrid(photoPaths = log.photoPaths)
            }
        }

        HorizontalDivider(color = BorderColor)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Lịch sử trạng thái".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary, letterSpacing = 0.6.sp,
                modifier = Modifier.padding(bottom = 12.dp))
            log.statusHistory.forEachIndexed { idx, history ->
                StatusTimelineItem(history = history, isLast = idx == log.statusHistory.lastIndex)
            }
        }

        if (log.canUpdateStatus) {
            HorizontalDivider(color = BorderColor)
            if (!state.showUpdateForm) {
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Cập nhật trạng thái".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(12.dp))

                    Text("Trạng thái mới *", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(MaintenanceStatus.RESOLVED, MaintenanceStatus.UNRESOLVED).forEach { s ->
                            val selected = state.selectedStatus == s
                            val (bg, fg) = when (s) {
                                MaintenanceStatus.RESOLVED -> Pair(GreenLight, GreenPrimary)
                                else                       -> Pair(RedLight, RedColor)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp)
                    .height(if (history.photoPaths.isNotEmpty()) 100.dp else 60.dp)
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
                Box(modifier = Modifier.clip(CircleShape).background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
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
                model = uri, contentDescription = null,
                modifier = Modifier.size(size.dp).clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { expandedUri = uri },
                contentScale = ContentScale.Crop
            )
        }
    }
    if (expandedUri != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { expandedUri = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { expandedUri = null }) {
                AsyncImage(
                    model = expandedUri, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────
@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        Text(value, fontSize = 14.sp, lineHeight = 20.sp)
    }
}