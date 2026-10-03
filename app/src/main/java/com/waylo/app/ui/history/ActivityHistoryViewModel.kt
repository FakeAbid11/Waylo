package com.waylo.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.common.WorkoutStatisticsCalculator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed interface ActivityHistoryUiState {
    data object Loading : ActivityHistoryUiState
    data object Empty : ActivityHistoryUiState
    data object Error : ActivityHistoryUiState
    data class Content(val sections: List<ActivitySection>) : ActivityHistoryUiState
}

data class ActivitySection(
    val label: String,
    val entries: List<ActivityEntry>,
)

data class ActivityEntry(
    val id: Long,
    val timestampLabel: String,
    val distanceMeters: Double,
    val activeMillis: Long,
    val averagePaceSecondsPerKm: Double?,
    val walkSteps: Long?,
)

class ActivityHistoryViewModel(
    private val repository: WalkingRepository,
    private val now: () -> Long,
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope = stateScope ?: viewModelScope
    private val _uiState = MutableStateFlow<ActivityHistoryUiState>(ActivityHistoryUiState.Loading)
    val uiState: StateFlow<ActivityHistoryUiState> = _uiState.asStateFlow()

    private var historyJob: Job? = null

    init {
        observeHistory()
    }

    fun retry() {
        observeHistory()
    }

    private fun observeHistory() {
        historyJob?.cancel()
        _uiState.value = ActivityHistoryUiState.Loading
        historyJob = scope.launch {
            repository.observeCompletedSessions()
                .map { sessions -> sessions.toUiState() }
                .catch { emit(ActivityHistoryUiState.Error) }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun List<WalkingSession>.toUiState(): ActivityHistoryUiState {
        if (isEmpty()) return ActivityHistoryUiState.Empty
        val zone = zoneId()
        val nowMillis = now()
        val today = WayloDateFormatter.localDate(nowMillis, zone)
        val sections = mutableListOf<ActivitySection>()
        forEach { session ->
            val day = WayloDateFormatter.localDate(session.startMillis, zone)
            val label = WayloDateFormatter.dayLabel(day, today)
            val entry = ActivityEntry(
                id = session.id,
                timestampLabel = WayloDateFormatter.historyTimestamp(session.startMillis, nowMillis, zone),
                distanceMeters = session.distanceMeters,
                activeMillis = session.activeMillis,
                averagePaceSecondsPerKm = WorkoutStatisticsCalculator.averagePaceSecondsPerKm(
                    distanceMeters = session.distanceMeters,
                    activeMillis = session.activeMillis,
                ),
                walkSteps = session.walkStepCount,
            )
            val last = sections.lastOrNull()
            if (last != null && last.label == label) {
                sections[sections.size - 1] = last.copy(entries = last.entries + entry)
            } else {
                sections += ActivitySection(label = label, entries = listOf(entry))
            }
        }
        return ActivityHistoryUiState.Content(sections)
    }

    companion object {
        fun factory(
            repository: WalkingRepository,
            now: () -> Long,
            zoneId: () -> ZoneId = { ZoneId.systemDefault() },
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActivityHistoryViewModel(
                    repository = repository,
                    now = now,
                    zoneId = zoneId,
                )
            }
        }
    }
}
