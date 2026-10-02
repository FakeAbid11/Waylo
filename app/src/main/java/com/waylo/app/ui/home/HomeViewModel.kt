package com.waylo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.data.step.StepRepository
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.DailyGoal
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.UserProgress
import com.waylo.app.domain.model.WalkingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val greeting: String = "Good evening",
    val stepState: DailyStepState = DailyStepState(),
    val progress: UserProgress = UserProgress.empty(),
    val todayDistanceKm: Double = 0.0,
    val todayWalkingMinutes: Int = 0,
    val walkState: WalkingState = WalkingState.Idle,
) {
    val goal: DailyGoal
        get() = stepState.asDailyGoal()

    val todaySteps: Int
        get() = goal.completedSteps

    val isStartWalkAvailable: Boolean
        get() = walkState != WalkingState.Stopping

    val startWalkLabel: String
        get() = if (walkState == WalkingState.Idle || walkState == WalkingState.Completed) {
            "Start Walk"
        } else {
            "Return to Walk"
        }
}

class HomeViewModel(
    private val stepRepository: StepRepository,
    private val walkingRepository: WalkingRepository,
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        stepRepository.state,
        walkingRepository.status,
    ) { stepState, walkStatus ->
        HomeUiState(
            stepState = stepState,
            walkState = walkStatus.state,
        )
    }.stateIn(
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
        fun factory(
            stepRepository: StepRepository,
            walkingRepository: WalkingRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomeViewModel(stepRepository, walkingRepository) }
            }
    }
}
