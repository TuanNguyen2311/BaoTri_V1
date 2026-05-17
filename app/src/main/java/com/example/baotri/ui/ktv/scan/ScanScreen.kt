package com.example.baotri.ui.ktv.scan

import android.Manifest
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.usecase.device.GetDeviceByCodeUseCase
import com.example.baotri.ui.shared.theme.*
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────
data class ScanUiState(
    val manualCode: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    val navigateToDevice: Long? = null,
    val flashOn: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val getDeviceByCode: GetDeviceByCodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var lastScanned = ""

    fun onQrScanned(code: String) {
        if (code == lastScanned || _state.value.navigateToDevice != null) return
        lastScanned = code
        findDevice(code)
    }

    fun onManualCodeChange(v: String) = _state.update { it.copy(manualCode = v, error = null) }

    fun searchManual() {
        val code = _state.value.manualCode.trim()
        if (code.isBlank()) return
        findDevice(code)
    }

    private fun findDevice(code: String) = viewModelScope.launch {
        _state.update { it.copy(isSearching = true, error = null) }
        getDeviceByCode(code)
            .onSuccess { device ->
                _state.update { it.copy(isSearching = false, navigateToDevice = device.id) }
            }
            .onFailure { e ->
                lastScanned = ""
                _state.update { it.copy(isSearching = false, error = e.message) }
            }
    }

    fun toggleFlash() = _state.update { it.copy(flashOn = !it.flashOn) }
    fun clearNav() { _state.update { it.copy(navigateToDevice = null) }; lastScanned = "" }
}

// ── Screen ────────────────────────────────────────────────────
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevice: (Long) -> Unit,
    vm: ScanViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(state.navigateToDevice) {
        state.navigateToDevice?.let { id -> onNavigateToDevice(id); vm.clearNav() }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── Camera Viewfinder ──────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (cameraPermission.status.isGranted) {
                CameraPreview(
                    flashOn = state.flashOn,
                    onQrDetected = vm::onQrScanned
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White.copy(.5f),
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Cần quyền truy cập Camera", color = Color.White.copy(.7f))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                            Text("Cấp quyền")
                        }
                    }
                }
            }

            // Topbar overlay
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack)
                Text("Scan thiết bị", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                CircleIconButton(
                    icon = if (state.flashOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                    onClick = vm::toggleFlash
                )
            }

            // Scan brackets
            ScanBrackets()

            // Hint
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(.55f)).padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Đưa mã vào khung để quét", color = Color.White, fontSize = 12.sp)
                }
            }

            // Loading overlay
            if (state.isSearching) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(.4f)),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00D49B))
                }
            }
        }

        // ── Bottom Panel ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderColor))
                Text("hoặc nhập thủ công", color = TextTertiary, fontSize = 12.sp)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderColor))
            }

            Spacer(Modifier.height(12.dp))

            Text("Mã thiết bị", style = MaterialTheme.typography.labelMedium,
                color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.manualCode,
                    onValueChange = vm::onManualCodeChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("VD: TBM-001", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Default.QrCode, null, tint = TextTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchManual() }),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor)
                )
                Button(
                    onClick = vm::searchManual,
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tìm")
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = RedColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(Color.Black.copy(.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ScanBrackets() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(200.dp)) {
            val bracketColor = Color(0xFF00D49B)
            val bracketSize = 28.dp
            val thickness = 3.dp
            // TL
            Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart)
                .border(width = 0.dp, color = Color.Transparent)
            ) {
                Box(modifier = Modifier.width(thickness).fillMaxHeight().background(bracketColor))
                Box(modifier = Modifier.height(thickness).fillMaxWidth().background(bracketColor))
            }
            // TR
            Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd)) {
                Box(modifier = Modifier.width(thickness).fillMaxHeight().align(Alignment.TopEnd).background(bracketColor))
                Box(modifier = Modifier.height(thickness).fillMaxWidth().background(bracketColor))
            }
            // BL
            Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart)) {
                Box(modifier = Modifier.width(thickness).fillMaxHeight().background(bracketColor))
                Box(modifier = Modifier.height(thickness).fillMaxWidth().align(Alignment.BottomStart).background(bracketColor))
            }
            // BR
            Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd)) {
                Box(modifier = Modifier.width(thickness).fillMaxHeight().align(Alignment.TopEnd).background(bracketColor))
                Box(modifier = Modifier.height(thickness).fillMaxWidth().align(Alignment.BottomStart).background(bracketColor))
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(flashOn: Boolean, onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(flashOn) { camera?.cameraControl?.enableTorch(flashOn) }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val scanner = BarcodeScanning.getClient()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            imageProxy.image?.let { img ->
                                val inputImage = InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { onQrDetected(it) }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } ?: imageProxy.close()
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, analysis
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
