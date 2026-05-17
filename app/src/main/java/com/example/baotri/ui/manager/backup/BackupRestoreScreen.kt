package com.example.baotri.ui.manager.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.BackupHistory
import com.example.baotri.domain.repository.BackupRepository
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*
import com.example.baotri.util.DateUtil
import com.example.baotri.util.toReadableSize
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class BackupUiState(
    val history: List<BackupHistory> = emptyList(),
    val latestBackup: BackupHistory? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val shareLocal: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepo: BackupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepo.getBackupHistory().collect { list ->
                _state.update { it.copy(history = list) }
            }
        }
        viewModelScope.launch {
            val latest = backupRepo.getLatestBackup()
            _state.update { it.copy(latestBackup = latest) }
        }
    }

    fun onDestinationChange(local: Boolean) = _state.update { it.copy(shareLocal = local) }

    fun export() = viewModelScope.launch {
        _state.update { it.copy(isExporting = true, error = null, successMessage = null) }
        try {
            val data = backupRepo.exportBackup()
            val timestamp = DateUtil.format(System.currentTimeMillis()).replace("/", "")
            val time = DateUtil.formatTime(System.currentTimeMillis()).replace(":", "")
            val fileName = "backup_${timestamp}_${time}.btdb"

            if (_state.value.shareLocal) {
                // Save to Downloads
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(data) }
                backupRepo.recordBackup("EXPORT", fileName, data.size.toLong(), "local")
                _state.update { it.copy(
                    isExporting = false,
                    successMessage = "Đã lưu: ${file.absolutePath}",
                    latestBackup = backupRepo.getLatestBackup()
                )}
            } else {
                // Share via Intent
                val dir = context.cacheDir
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(data) }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Chia sẻ file backup")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                backupRepo.recordBackup("EXPORT", fileName, data.size.toLong(), "share")
                _state.update { it.copy(isExporting = false, successMessage = "Đã mở hộp thoại chia sẻ") }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isExporting = false, error = "Xuất thất bại: ${e.message}") }
        }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(isImporting = true, error = null, successMessage = null) }
        try {
            val data = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: throw Exception("Không đọc được file")
            backupRepo.importBackup(data)
            val fileName = uri.lastPathSegment ?: "unknown.btdb"
            backupRepo.recordBackup("IMPORT", fileName, data.size.toLong(), "local")
            _state.update { it.copy(isImporting = false, successMessage = "Khôi phục dữ liệu thành công!") }
        } catch (e: Exception) {
            _state.update { it.copy(
                isImporting = false,
                error = if (e.message?.contains("tampered") == true)
                    "File không hợp lệ hoặc đã bị chỉnh sửa. Vui lòng dùng file backup gốc."
                else "Khôi phục thất bại: ${e.message}"
            )}
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}

// ── Screen ─────────────────────────────────────────────────────

@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    vm: BackupViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> uri?.let { vm.import(it) } }

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = "Backup & Khôi phục",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info banner
            item {
                InfoBox(
                    message = "Dữ liệu được mã hóa AES-256. File backup định dạng .btdb không thể đọc hoặc chỉnh sửa bên ngoài ứng dụng.",
                    icon = Icons.Default.Security,
                    type = InfoBoxType.INFO
                )
            }

            // Success/Error messages
            if (state.successMessage != null) {
                item {
                    InfoBox(message = state.successMessage!!, icon = Icons.Default.CheckCircle, type = InfoBoxType.SUCCESS)
                }
            }
            if (state.error != null) {
                item {
                    InfoBox(message = state.error!!, icon = Icons.Default.Error, type = InfoBoxType.ERROR)
                }
            }

            // Export section
            item {
                Text("XUẤT DỮ LIỆU (EXPORT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary, letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(bottom = 8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        // Card header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(GreenLight),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.CloudUpload, null, tint = GreenPrimary, modifier = Modifier.size(22.dp)) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Xuất file backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Toàn bộ thiết bị, log và tài khoản đóng gói vào 1 file .btdb mã hóa",
                                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                            }
                        }
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Last backup info
                            if (state.latestBackup != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Backup gần nhất", fontSize = 11.sp, color = TextTertiary)
                                        Text(DateUtil.formatDateTime(state.latestBackup!!.createdAt),
                                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(state.latestBackup!!.fileSizeBytes.toReadableSize(),
                                        fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            // Destination selector
                            Text("Lưu vào", style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DestinationChip(
                                    label = "Bộ nhớ máy",
                                    icon = Icons.Default.PhoneAndroid,
                                    selected = state.shareLocal,
                                    onClick = { vm.onDestinationChange(true) },
                                    modifier = Modifier.weight(1f)
                                )
                                DestinationChip(
                                    label = "Chia sẻ app",
                                    icon = Icons.Default.Share,
                                    selected = !state.shareLocal,
                                    onClick = { vm.onDestinationChange(false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LoadingButton(
                                text = "Xuất file backup ngay",
                                loading = state.isExporting,
                                onClick = vm::export,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Import section
            item {
                Text("KHÔI PHỤC DỮ LIỆU (IMPORT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary, letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(bottom = 8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.CloudDownload, null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(22.dp)) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Khôi phục từ file backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Chọn file .btdb đã xuất trước đó để khôi phục toàn bộ dữ liệu",
                                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                            }
                        }
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(12.dp)) {
                            InfoBox(
                                message = "Thao tác này sẽ GHI ĐÈ toàn bộ dữ liệu hiện tại. Nên xuất backup trước khi khôi phục.",
                                icon = Icons.Default.Warning,
                                type = InfoBoxType.WARNING
                            )
                            Spacer(Modifier.height(10.dp))
                            LoadingButton(
                                text = "Chọn file .btdb để khôi phục",
                                loading = state.isImporting,
                                onClick = { importLauncher.launch("application/octet-stream") },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }

            // History
            if (state.history.isNotEmpty()) {
                item {
                    Text("LỊCH SỬ BACKUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                items(state.history) { h ->
                    val isExport = h.action == "EXPORT"
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isExport) GreenLight else Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isExport) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                                null,
                                tint = if (isExport) GreenPrimary else Color(0xFF1D4ED8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(h.fileName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${DateUtil.formatDateTime(h.createdAt)} · ${h.fileSizeBytes.toReadableSize()} · ${h.destination}",
                                fontSize = 11.sp, color = TextTertiary)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(99.dp))
                                .background(if (isExport) GreenLight else Color(0xFFDBEAFE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(if (isExport) "Export" else "Import", fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExport) GreenPrimary else Color(0xFF1D4ED8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationChip(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) GreenLight else MaterialTheme.colorScheme.background)
            .border(1.dp, if (selected) GreenPrimary else BorderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint = if (selected) GreenPrimary else TextSecondary,
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) GreenPrimary else TextSecondary)
    }
}
