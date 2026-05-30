package com.example.baotri.ui.manager.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.*
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.log.ClassifyDeviceAlertsUseCase
import com.example.baotri.domain.usecase.log.GetCurrentMonthRangeUseCase
import com.example.baotri.domain.usecase.log.GetManagerStatsUseCase
import com.example.baotri.domain.usecase.log.GetTopDevicesUseCase
import com.example.baotri.domain.usecase.log.GetWeeklyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagerDashboardUiState(
    val fullName: String = "",
    val stats: ManagerStats = ManagerStats(0, 0, 0, 0),
    val weeklyData: List<WeeklyLogCount> = emptyList(),
    val alerts: List<DeviceAlert> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ManagerDashboardViewModel @Inject constructor(
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val getStats: GetManagerStatsUseCase,
    private val getWeeklyStats: GetWeeklyStatsUseCase,
    private val getTopDevices: GetTopDevicesUseCase,
    private val getMonthRange: GetCurrentMonthRangeUseCase,
    private val classifyAlerts: ClassifyDeviceAlertsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ManagerDashboardUiState())
    val state: StateFlow<ManagerDashboardUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        val (_, fullName) = getCurrentSession()
        val stats = getStats()
        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = getMonthRange(now)
        val weekly = getWeeklyStats(monthStart, monthEnd)
        val topDevices = getTopDevices(monthStart, monthEnd)
        val alerts = classifyAlerts(topDevices, now)

        _state.value = ManagerDashboardUiState(
            fullName   = fullName,
            stats      = stats,
            weeklyData = weekly,
            alerts     = alerts,
            isLoading  = false
        )
    }
}