package com.example.baotri.ui.manager.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.DeviceIncident
import com.example.baotri.domain.model.WeeklyLogCount
import com.example.baotri.domain.usecase.log.GetReportSummaryUseCase
import com.example.baotri.domain.usecase.log.GetTopDevicesUseCase
import com.example.baotri.domain.usecase.log.GetWeeklyStatsUseCase
import com.example.baotri.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReportPeriod { WEEK, MONTH, QUARTER, YEAR }

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val summary: Map<String, Int> = emptyMap(),
    val weeklyData: List<WeeklyLogCount> = emptyList(),
    val topDevices: List<DeviceIncident> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getWeeklyStats: GetWeeklyStatsUseCase,
    private val getTopDevices: GetTopDevicesUseCase,
    private val getSummary: GetReportSummaryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    init { load(ReportPeriod.MONTH) }

    fun onPeriodChange(p: ReportPeriod) = viewModelScope.launch {
        _state.update { it.copy(period = p, isLoading = true) }
        load(p)
    }

    private fun load(period: ReportPeriod) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val (from, to) = when (period) {
            ReportPeriod.WEEK    -> Pair(now - 7 * 86_400_000L, now)
            ReportPeriod.MONTH   -> Pair(DateUtil.startOfMonth(now), DateUtil.endOfMonth(now))
            ReportPeriod.QUARTER -> Pair(now - 90 * 86_400_000L, now)
            ReportPeriod.YEAR    -> Pair(now - 365 * 86_400_000L, now)
        }
        val summary = getSummary(from, to)
        val weekly  = getWeeklyStats(DateUtil.startOfMonth(now), DateUtil.endOfMonth(now))
        val top     = getTopDevices(from, to)
        _state.update { it.copy(summary = summary, weeklyData = weekly, topDevices = top, isLoading = false) }
    }
}
