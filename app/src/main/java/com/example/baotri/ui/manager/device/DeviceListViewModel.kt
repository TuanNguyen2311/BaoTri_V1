package com.example.baotri.ui.manager.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.Device
import com.example.baotri.domain.usecase.device.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val devices: List<Device> = emptyList(),
    val query: String = "",
    val selectedArea: String = "Tất cả",
    val isLoading: Boolean = true
) {
    val filteredDevices: List<Device> get() =
        if (selectedArea == "Tất cả") devices
        else devices.filter { it.location.contains(selectedArea, ignoreCase = true) }
}

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val getAllDevices: GetAllDevicesUseCase,
    private val searchDevices: SearchDevicesUseCase,
    private val deleteDevice: DeleteDeviceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceListUiState())
    val state: StateFlow<DeviceListUiState> = _state.asStateFlow()
    private val _query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _query.debounce(300).flatMapLatest { q ->
                if (q.isBlank()) getAllDevices() else searchDevices(q)
            }.collect { list ->
                _state.update { it.copy(devices = list, isLoading = false) }
            }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
        _state.update { it.copy(query = q) }
    }

    fun onAreaChange(area: String) = _state.update { it.copy(selectedArea = area) }

    fun deleteDevice(deviceId: Long) = viewModelScope.launch {
        deleteDevice.invoke(deviceId)
    }
}
