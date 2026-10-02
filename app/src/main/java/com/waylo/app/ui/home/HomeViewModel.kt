package com.waylo.app.ui.home

import androidx.lifecycle.ViewModel
import com.waylo.app.domain.model.DailyGoal
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val greeting: String = "Good evening",
    val goal: DailyGoal = DailyGoal.empty(),
    val progress: UserProgress = UserProgress.empty(),
    val todayDistanceKm: Double = 0.0,
    val todayWalkingMinutes: Int = 0,
    val isStartWalkAvailable: Boolean = false,
) {
    val todaySteps: Int
        get() = goal.completedSteps
}

class HomeViewModel : ViewModel() {

    val uiState: StateFlow<HomeUiState> = MutableStateFlow(HomeUiState()).asStateFlow()
}
