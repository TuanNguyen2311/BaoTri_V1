package com.example.baotri.ui.ktv.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.*
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.device.GetDeviceByIdUseCase
import com.example.baotri.domain.usecase.log.SaveLogUseCase
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
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val getDeviceById: GetDeviceByIdUseCase,
    private val saveLog: SaveLogUseCase
) : ViewModel() {

    private val deviceId: Long = checkNotNull(savedStateHandle["deviceId"])
    private val logId: Long = savedStateHandle["logId"] ?: 0L
    private var userId: Long = -1L

    private val _state = MutableStateFlow(WriteLogUiState())
    val state: StateFlow<WriteLogUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val (sessionUserId, fullName) = getCurrentSession()
        userId = sessionUserId
        val device = getDeviceById(deviceId)
        _state.update {
            it.copy(
                deviceName      = device?.name ?: "",
                deviceCode      = device?.code ?: "",
                deviceLocation  = device?.location ?: "",
                performedByName = fullName
            )
        }
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
