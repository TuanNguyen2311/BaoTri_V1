package com.example.baotri.ui.manager.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.BackupHistory
import com.example.baotri.domain.usecase.backup.*
import com.example.baotri.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

sealed class BackupFileEvent {
    // Screen launches CreateDocument picker; user picks save location; Screen writes the bytes
    data class RequestSaveLocation(val suggestedFileName: String, val data: ByteArray) : BackupFileEvent()
    // Screen creates FileProvider URI and launches the system share chooser
    data class RequestShare(val filePath: String) : BackupFileEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val getHistory: GetBackupHistoryUseCase,
    private val getLatestBackup: GetLatestBackupUseCase,
    private val recordBackup: RecordBackupUseCase,
    private val prepareForSharing: PrepareBackupForSharingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _fileEvent = MutableSharedFlow<BackupFileEvent>(extraBufferCapacity = 1)
    val fileEvent: SharedFlow<BackupFileEvent> = _fileEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getHistory().collect { list -> _state.update { it.copy(history = list) } }
        }
        viewModelScope.launch {
            _state.update { it.copy(latestBackup = getLatestBackup()) }
        }
    }

    fun onDestinationChange(local: Boolean) = _state.update { it.copy(shareLocal = local) }

    fun export() = viewModelScope.launch {
        _state.update { it.copy(isExporting = true, error = null, successMessage = null) }
        exportBackup()
            .onSuccess { data ->
                val timestamp = DateUtil.format(System.currentTimeMillis()).replace("/", "")
                val time = DateUtil.formatTime(System.currentTimeMillis()).replace(":", "")
                val fileName = "backup_${timestamp}_${time}.btdb"
                try {
                    if (_state.value.shareLocal) {
                        // Serialization done — stop spinner, let user pick where to save
                        _state.update { it.copy(isExporting = false) }
                        _fileEvent.emit(BackupFileEvent.RequestSaveLocation(fileName, data))
                    } else {
                        val path = prepareForSharing(data, fileName)
                        recordBackup("EXPORT", fileName, data.size.toLong(), "share")
                        _fileEvent.emit(BackupFileEvent.RequestShare(path))
                        _state.update {
                            it.copy(isExporting = false, successMessage = "Đã mở hộp thoại chia sẻ",
                                latestBackup = getLatestBackup())
                        }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(isExporting = false, error = "Xuất thất bại: ${e.message}") }
                }
            }
            .onFailure { e ->
                _state.update { it.copy(isExporting = false, error = "Xuất thất bại: ${e.message}") }
            }
    }

    // Called by Screen after user picks a URI and the write succeeds
    fun onExportSaved(fileName: String, sizeBytes: Long) = viewModelScope.launch {
        recordBackup("EXPORT", fileName, sizeBytes, "local")
        _state.update { it.copy(successMessage = "Xuất file thành công!", latestBackup = getLatestBackup()) }
    }

    fun onExportError(message: String) = _state.update { it.copy(error = message) }

    fun import(data: ByteArray, fileName: String) = viewModelScope.launch {
        _state.update { it.copy(isImporting = true, error = null, successMessage = null) }
        importBackup(data)
            .onSuccess {
                recordBackup("IMPORT", fileName, data.size.toLong(), "local")
                _state.update { it.copy(isImporting = false, successMessage = "Khôi phục dữ liệu thành công!") }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        isImporting = false,
                        error = if (e.message?.contains("tampered") == true)
                            "File không hợp lệ hoặc đã bị chỉnh sửa. Vui lòng dùng file backup gốc."
                        else "Khôi phục thất bại: ${e.message}"
                    )
                }
            }
    }

    fun showImportError(message: String) = _state.update { it.copy(error = message) }
    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}