package com.example.baotri.ui.ktv.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.usecase.device.GetDeviceByCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val manualCode: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    val navigateToDevice: Long? = null,
    val flashOn: Boolean = false,
    val isPaused: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val getDeviceByCode: GetDeviceByCodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var lastScanned = ""

    fun onQrScanned(code: String) {
        // Bỏ qua nếu đang tạm dừng hoặc đã xử lý code này rồi
        if (_state.value.isPaused) return
        if (code == lastScanned || _state.value.navigateToDevice != null) return
        lastScanned = code
        findDevice(code)
    }

    fun onManualCodeChange(v: String) = _state.update { it.copy(manualCode = v, error = null) }

    fun searchManual() {
        val code = _state.value.manualCode.trim()
        if (code.isBlank()) return
        _state.update { it.copy(isPaused = false) }
        findDevice(code)
    }

    fun resumeScan() {
        lastScanned = ""
        _state.update { it.copy(isPaused = false, error = null) }
    }

    private fun findDevice(code: String) = viewModelScope.launch {
        _state.update { it.copy(isSearching = true, error = null) }
        getDeviceByCode(code)
            .onSuccess { device ->
                // Dừng scan sau khi tìm thấy — chờ navigate
                _state.update { it.copy(isSearching = false, navigateToDevice = device.id, isPaused = true) }
            }
            .onFailure { e ->
                lastScanned = ""
                // Dừng scan sau lỗi — chờ user tap để thử lại
                _state.update { it.copy(isSearching = false, error = e.message, isPaused = true) }
            }
    }

    fun pauseCamera() = _state.update { it.copy(isPaused = true, error = null) }

    fun toggleFlash() = _state.update { it.copy(flashOn = !it.flashOn) }
    fun clearNav() { _state.update { it.copy(navigateToDevice = null) }; lastScanned = "" }
}