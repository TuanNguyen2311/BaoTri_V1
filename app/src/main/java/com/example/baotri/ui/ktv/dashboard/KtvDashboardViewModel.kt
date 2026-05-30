package com.example.baotri.ui.ktv.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baotri.domain.model.KtvStats
import com.example.baotri.domain.model.MaintenanceLog
import com.example.baotri.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.baotri.domain.usecase.log.GetKtvStatsUseCase
import com.example.baotri.domain.usecase.log.GetUserLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KtvDashboardUiState(
    val fullName: String = "",
    val stats: KtvStats = KtvStats(0, 0),
    val recentLogs: List<MaintenanceLog> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class KtvDashboardViewModel @Inject constructor(
    private val getCurrentSession: GetCurrentSessionUseCase,
    private val getKtvStats: GetKtvStatsUseCase,
    private val getUserLogs: GetUserLogsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(KtvDashboardUiState())
    val state: StateFlow<KtvDashboardUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val (userId, fullName) = getCurrentSession()

        // Load recent logs
        getUserLogs(userId)
            .combine(flowOf(getKtvStats(userId))) { logs, stats ->
                KtvDashboardUiState(
                    fullName   = fullName,
                    stats      = stats,
                    recentLogs = logs.take(10),
                    isLoading  = false
                )
            }
            .collect { _state.value = it }
    }

    fun refresh() = load()
}
