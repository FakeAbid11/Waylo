package com.waylo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.data.step.StepRepository
import com.waylo.app.domain.model.DailyGoal
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val greeting: String = "Good evening",
    val stepState: DailyStepState = DailyStepState(),
    val progress: UserProgress = UserProgress.empty(),
    val todayDistanceKm: Double = 0.0,
    val todayWalkingMinutes: Int = 0,
    val isStartWalkAvailable: Boolean = false,
) {
    val goal: DailyGoal
        get() = stepState.asDailyGoal()

    val todaySteps: Int
        get() = goal.completedSteps
}

class HomeViewModel(
    private val stepRepository: StepRepository,
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = stepRepository.state
        .map { stepState -> HomeUiState(stepState = stepState) }
        .stateIn(
            scope = stateScope ?: viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState(),
        )

    init {
        stepRepository.start()
    }

    fun refresh() {
        stepRepository.refresh()
    }

    override fun onCleared() {
        stepRepository.stop()
        super.onCleared()
    }

    companion object {
        fun factory(stepRepository: StepRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomeViewModel(stepRepository) }
            }
    }
}
