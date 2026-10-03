package com.waylo.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.data.progression.ProgressionRepository
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val progress: UserProgress = UserProgress.empty(),
)

class ProgressViewModel(
    progression: ProgressionRepository? = null,
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = (progression?.progress ?: flowOf(UserProgress.empty()))
        .map { progress -> ProgressUiState(progress = progress) }
        .stateIn(
            scope = stateScope ?: viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ProgressUiState(),
        )

    companion object {
        fun factory(progression: ProgressionRepository?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ProgressViewModel(progression = progression) }
            }
    }
}
