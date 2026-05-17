package com.example.baotri.ui.manager.device

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Device
import com.example.baotri.domain.repository.DeviceRepository
import com.example.baotri.domain.usecase.device.SaveDeviceUseCase
import com.example.baotri.util.DateUtil
import com.example.baotri.util.QRUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.baotri.ui.shared.components.*
import com.example.baotri.ui.shared.theme.*

data class AddEditDeviceUiState(
    val isEdit: Boolean = false,
    val code: String = "",
    val name: String = "",
    val category: String = "",
    val location: String = "",
    val buyDateText: String = "",
    val warrantyDateText: String = "",
    val photoUri: String? = null,
    val notes: String = "",
    val qrPath: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

val DEVICE_CATEGORIES = listOf("Khí nén", "Băng tải", "Bơm", "Nhiệt / Lò sấy", "Điện", "Lạnh", "Thủy lực", "Khác")

@HiltViewModel
class AddEditDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepo: DeviceRepository,
    private val saveDevice: SaveDeviceUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val deviceId: Long = savedStateHandle["deviceId"] ?: 0L
    private val _state = MutableStateFlow(AddEditDeviceUiState())
    val state: StateFlow<AddEditDeviceUiState> = _state.asStateFlow()

    init { if (deviceId != 0L) loadDevice() }

    private fun loadDevice() = viewModelScope.launch {
        val device = deviceRepo.getDeviceById(deviceId) ?: return@launch
        _state.update { it.copy(
            isEdit          = true,
            code            = device.code,
            name            = device.name,
            category        = device.category,
            location        = device.location,
            buyDateText     = device.buyDate?.let { DateUtil.format(it) } ?: "",
            warrantyDateText = device.warrantyDate?.let { DateUtil.format(it) } ?: "",
            photoUri        = device.photoPath,
            notes           = device.notes ?: "",
            qrPath          = device.qrPath
        )}
    }

    fun onCodeChange(v: String)     = _state.update { it.copy(code = v.uppercase(), error = null) }
    fun onNameChange(v: String)     = _state.update { it.copy(name = v, error = null) }
    fun onCategoryChange(v: String) = _state.update { it.copy(category = v) }
    fun onLocationChange(v: String) = _state.update { it.copy(location = v) }
    fun onBuyDateChange(v: String)  = _state.update { it.copy(buyDateText = v) }
    fun onWarrantyChange(v: String) = _state.update { it.copy(warrantyDateText = v) }
    fun onPhotoChange(uri: String)  = _state.update { it.copy(photoUri = uri) }
    fun onNotesChange(v: String)    = _state.update { it.copy(notes = v) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        val device = Device(
            id           = deviceId,
            code         = s.code.trim(),
            name         = s.name.trim(),
            category     = s.category,
            location     = s.location.trim(),
            buyDate      = DateUtil.parse(s.buyDateText),
            warrantyDate = DateUtil.parse(s.warrantyDateText),
            photoPath    = s.photoUri,
            qrPath       = s.qrPath,
            notes        = s.notes.ifBlank { null }
        )
        saveDevice(device)
            .onSuccess { savedId ->
                // Generate QR
                val qrPath = QRUtil.saveQrToFile(context, s.code)
                deviceRepo.updateQrPath(savedId, qrPath)
                _state.update { it.copy(isLoading = false, saved = true, qrPath = qrPath) }
            }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }
}

// ── Screen ─────────────────────────────────────────────────────


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeviceScreen(
    onNavigateBack: () -> Unit,
    vm: AddEditDeviceViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { vm.onPhotoChange(it.toString()) }
    }

    LaunchedEffect(state.saved) { if (state.saved) onNavigateBack() }

    Scaffold(
        topBar = {
            BaoTriTopBar(
                title = if (state.isEdit) "Sửa thiết bị" else "Thêm thiết bị",
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (state.isEdit) {
                        TextButton(onClick = {}) {
                            Text("Xóa", color = RedColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // Photo hero
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(GreenPrimary, androidx.compose.ui.graphics.Color(0xFF1A9B7B))
                        )
                    ).clickable { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (state.photoUri != null) {
                    AsyncImage(model = state.photoUri, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Settings, null,
                        tint = androidx.compose.ui.graphics.Color.White.copy(.7f),
                        modifier = Modifier.size(48.dp))
                }
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .clip(RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Color.Black.copy(.4f))
                    .padding(6.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null,
                        tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Code
                FormField(label = "Mã thiết bị", value = state.code,
                    onValueChange = vm::onCodeChange, placeholder = "VD: TBM-001", required = true)
                // Name
                FormField(label = "Tên thiết bị", value = state.name,
                    onValueChange = vm::onNameChange, placeholder = "VD: Máy nén khí #01", required = true)
                // Category dropdown
                Spacer(Modifier.height(4.dp))
                Text("Danh mục *", style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.category.ifBlank { "Chọn danh mục..." },
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor,
                            unfocusedTextColor = if (state.category.isBlank()) TextTertiary else MaterialTheme.colorScheme.onBackground
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DEVICE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) },
                                onClick = { vm.onCategoryChange(cat); expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Location
                FormField(label = "Vị trí", value = state.location,
                    onValueChange = vm::onLocationChange, placeholder = "VD: Khu A – Xưởng 1", required = true)
                // Date row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormField(label = "Ngày mua", value = state.buyDateText,
                            onValueChange = vm::onBuyDateChange, placeholder = "dd/MM/yyyy")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FormField(label = "Hạn bảo hành", value = state.warrantyDateText,
                            onValueChange = vm::onWarrantyChange, placeholder = "dd/MM/yyyy",
                            textColor = if (state.warrantyDateText.isNotBlank() &&
                                com.example.baotri.util.DateUtil.parse(state.warrantyDateText)?.let { it < System.currentTimeMillis() } == true)
                                AmberColor else null)
                    }
                }
                // Notes
                Spacer(Modifier.height(4.dp))
                Text("Ghi chú", style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = state.notes, onValueChange = vm::onNotesChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    placeholder = { Text("Ghi chú thêm về thiết bị...", color = TextTertiary) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor)
                )
                Spacer(Modifier.height(16.dp))
                // QR section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.qrPath != null && state.code.isNotBlank()) {
                                val bmp = remember(state.code) {
                                    runCatching { QRUtil.generateQrBitmap(state.code, 200) }.getOrNull()
                                }
                                bmp?.let {
                                    androidx.compose.foundation.Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                }
                            } else {
                                Icon(Icons.Default.QrCode, null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mã QR thiết bị", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (state.code.isBlank()) "Nhập mã thiết bị để sinh QR"
                                else "${state.code} · Tự động sinh sau khi lưu",
                                fontSize = 11.sp, color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {},
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(99.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary)
                            ) {
                                Icon(Icons.Default.Print, null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("In QR (PDF)", color = PurplePrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (state.error != null) {
                    Text(state.error!!, color = RedColor, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onNavigateBack, modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) { Text("Hủy", color = TextSecondary) }
                    LoadingButton(
                        text = "Lưu thiết bị", loading = state.isLoading,
                        onClick = vm::save, modifier = Modifier.weight(2f),
                        containerColor = PurplePrimary
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FormField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String = "", required: Boolean = false,
    textColor: androidx.compose.ui.graphics.Color? = null
) {
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        if (required) Text(" *", color = RedColor, fontSize = 12.sp)
    }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextTertiary) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PurplePrimary, unfocusedBorderColor = BorderColor,
            focusedTextColor = textColor ?: MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = textColor ?: MaterialTheme.colorScheme.onBackground
        )
    )
    Spacer(Modifier.height(10.dp))
}
