package com.waylo.app.ui.progress

import androidx.lifecycle.ViewModel
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProgressUiState(
    val progress: UserProgress = UserProgress.empty(),
)

class ProgressViewModel : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = MutableStateFlow(ProgressUiState()).asStateFlow()
}
