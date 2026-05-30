package com.example.baotri.ui.ktv.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.*
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.device.GetDeviceByIdUseCase
import com.example.baotri.domain.usecase.log.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceDetailUiState(
    val device: Device? = null,
    val logs: List<MaintenanceLog> = emptyList(),
    val isLoading: Boolean = true,
    val isKtv: Boolean = false
)

data class LogDetailSheetState(
    val isVisible: Boolean = false,
    val log: MaintenanceLog? = null,
    val isLoadingHistory: Boolean = false,
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
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val getDeviceById: GetDeviceByIdUseCase,
    private val getDeviceLogs: GetDeviceLogsUseCase,
    private val getLogWithHistory: GetLogWithHistoryUseCase,
    private val updateLogStatus: UpdateLogStatusUseCase
) : ViewModel() {

    private val deviceId: Long = checkNotNull(savedStateHandle["deviceId"])

    private val _state = MutableStateFlow(DeviceDetailUiState())
    val state: StateFlow<DeviceDetailUiState> = _state.asStateFlow()

    private val _sheetState = MutableStateFlow(LogDetailSheetState())
    val sheetState: StateFlow<LogDetailSheetState> = _sheetState.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val session = getCurrentSession()
        val device  = getDeviceById(deviceId)
        getDeviceLogs(deviceId).collect { logs ->
            _state.value = DeviceDetailUiState(
                device    = device,
                logs      = logs,
                isLoading = false,
                isKtv     = session.role == Role.TECHNICIAN.name
            )
        }
    }

    fun openLogDetail(logId: Long) = viewModelScope.launch {
        _sheetState.update { it.copy(isVisible = true, isLoadingHistory = true, showUpdateForm = false) }
        val logWithHistory = getLogWithHistory(logId)
        _sheetState.update { it.copy(log = logWithHistory, isLoadingHistory = false) }
    }

    fun closeSheet() { _sheetState.value = LogDetailSheetState() }

    fun showUpdateForm() = _sheetState.update {
        it.copy(showUpdateForm = true, selectedStatus = MaintenanceStatus.RESOLVED,
            updateNote = "", updatePhotos = emptyList(), updateError = null)
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
        val (userId, fullName) = getCurrentSession()
        updateLogStatus(
            logId           = logId,
            newStatus       = sheet.selectedStatus,
            changedByUserId = userId,
            changedByName   = fullName,
            note            = sheet.updateNote,
            photoPaths      = sheet.updatePhotos
        ).onSuccess {
            val updated = getLogWithHistory(logId)
            _sheetState.update { it.copy(isUpdating = false, showUpdateForm = false, log = updated) }
        }.onFailure { e ->
            _sheetState.update { it.copy(isUpdating = false, updateError = e.message) }
        }
    }
}
