package com.waylo.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.data.achievement.AchievementRepository
import com.waylo.app.data.progression.ProgressionRepository
import com.waylo.app.domain.model.AchievementSummary
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val progress: UserProgress = UserProgress.empty(),
    val achievementsUnlocked: Int = 0,
    val achievementsTotal: Int = AchievementCatalog.definitions.size,
)

class ProgressViewModel(
    progression: ProgressionRepository? = null,
    achievements: AchievementRepository? = null,
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private val progressFlow: Flow<UserProgress> =
        progression?.progress ?: flowOf(UserProgress.empty())

    private val achievementSummary: Flow<AchievementSummary?> =
        achievements?.summary ?: flowOf(null)

    val uiState: StateFlow<ProgressUiState> = combine(
        progressFlow,
        achievementSummary,
    ) { progress, summary ->
        ProgressUiState(
            progress = progress,
            achievementsUnlocked = summary?.unlockedCount ?: 0,
            achievementsTotal = summary?.totalCount ?: AchievementCatalog.definitions.size,
        )
    }.stateIn(
        scope = stateScope ?: viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProgressUiState(),
    )

    companion object {
        fun factory(
            progression: ProgressionRepository?,
            achievements: AchievementRepository? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ProgressViewModel(
                        progression = progression,
                        achievements = achievements,
                    )
                }
            }
    }
}
