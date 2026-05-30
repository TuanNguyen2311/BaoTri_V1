package com.example.baotri.ui.ktv.scan

import android.Manifest
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.baotri.ui.shared.theme.*
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevice: (Long) -> Unit,
    vm: ScanViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var showManualSheet by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.navigateToDevice) {
        state.navigateToDevice?.let { id ->
            showManualSheet = false
            onNavigateToDevice(id)
            vm.clearNav()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = state.isPaused && !state.isSearching) { vm.resumeScan() }
    ) {
        // ── Camera ──────────────────────────────────────────────
        if (!state.isPaused && cameraPermission.status.isGranted) {
            CameraPreview(flashOn = state.flashOn, onQrDetected = vm::onQrScanned)
        } else if (!cameraPermission.status.isGranted) {
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null,
                        tint = Color.White.copy(.5f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Cần quyền truy cập Camera", color = Color.White.copy(.7f))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Cấp quyền") }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)))
        }

        // ── Topbar overlay ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(icon = Icons.Default.ArrowBack, onClick = onNavigateBack)
            Text("Scan thiết bị", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.isPaused) {
                    CircleIconButton(
                        icon = if (state.flashOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                        onClick = vm::toggleFlash
                    )
                }
                CircleIconButton(
                    icon = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    onClick = { if (state.isPaused) vm.resumeScan() else vm.pauseCamera() }
                )
            }
        }

        // ── Scan brackets + hint + manual button ─────────────────
        if (!state.isPaused) {
            ScanBrackets()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(.55f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Đưa mã vào khung để quét", color = Color.White, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { showManualSheet = true },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.6f)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nhập mã thủ công", fontSize = 13.sp)
                }
            }
        }

        // ── Pause overlay ────────────────────────────────────────
        if (state.isPaused && !state.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.TouchApp, null,
                        tint = Color.White, modifier = Modifier.size(48.dp))
                    Text("Chạm để tiếp tục quét",
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (state.error != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFB91C1C).copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(state.error!!, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showManualSheet = true },
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.6f)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nhập mã thủ công", fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Loading overlay ──────────────────────────────────────
        if (state.isSearching) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00D49B))
            }
        }

        // ── Custom manual input sheet (không dùng experimental API) ──
        AnimatedVisibility(
            visible = showManualSheet,
            enter = fadeIn(tween(200)),
            exit  = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            // Backdrop — tap để đóng
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { keyboard?.hide(); showManualSheet = false }
            )
        }

        AnimatedVisibility(
            visible = showManualSheet,
            enter = slideInVertically(tween(280)) { it },
            exit  = slideOutVertically(tween(220)) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            // Wrapper fillMaxSize + imePadding — khi keyboard xuất hiện,
            // box co lại từ dưới, đẩy content (BottomCenter) lên trên keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 24.dp)
                        // Ngăn click xuyên qua sheet xuống backdrop
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {},
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .width(40.dp).height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(.2f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))

                    Text("Nhập mã thiết bị",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.manualCode,
                            onValueChange = vm::onManualCodeChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("VD: TBM-001", color = TextTertiary) },
                            leadingIcon = { Icon(Icons.Default.QrCode, null, tint = TextTertiary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide()
                                vm.searchManual()
                            }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        Button(
                            onClick = { keyboard?.hide(); vm.searchManual() },
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null,
                                tint = RedColor, modifier = Modifier.size(16.dp))
                            Text(state.error!!, color = RedColor, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
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
            val c = Color(0xFF00D49B); val s = 28.dp; val t = 3.dp
            Box(Modifier.size(s).align(Alignment.TopStart)) {
                Box(Modifier.width(t).fillMaxHeight().background(c))
                Box(Modifier.height(t).fillMaxWidth().background(c))
            }
            Box(Modifier.size(s).align(Alignment.TopEnd)) {
                Box(Modifier.width(t).fillMaxHeight().align(Alignment.TopEnd).background(c))
                Box(Modifier.height(t).fillMaxWidth().background(c))
            }
            Box(Modifier.size(s).align(Alignment.BottomStart)) {
                Box(Modifier.width(t).fillMaxHeight().background(c))
                Box(Modifier.height(t).fillMaxWidth().align(Alignment.BottomStart).background(c))
            }
            Box(Modifier.size(s).align(Alignment.BottomEnd)) {
                Box(Modifier.width(t).fillMaxHeight().align(Alignment.TopEnd).background(c))
                Box(Modifier.height(t).fillMaxWidth().align(Alignment.BottomStart).background(c))
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
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(flashOn) { camera?.cameraControl?.enableTorch(flashOn) }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown(); cameraProvider?.unbindAll() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val scanner = BarcodeScanning.getClient()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            imageProxy.image?.let { img ->
                                val input = InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(input)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { onQrDetected(it) }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } ?: imageProxy.close()
                        }
                    }
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}