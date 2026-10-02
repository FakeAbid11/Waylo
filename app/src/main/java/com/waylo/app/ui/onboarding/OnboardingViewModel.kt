package com.waylo.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.data.preferences.WayloPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val wayloPreferences: WayloPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val completed = wayloPreferences.onboardingCompleted.first()
            if (completed) {
                _uiState.update { it.copy(onboardingCompleted = true) }
            }
        }
    }

    fun nextPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.currentPage + 1).coerceAtMost(state.totalPages - 1))
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.currentPage - 1).coerceAtLeast(0))
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            wayloPreferences.setOnboardingCompleted()
            _uiState.update { it.copy(onboardingCompleted = true) }
        }
    }

    companion object {
        fun factory(wayloPreferences: WayloPreferences): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { OnboardingViewModel(wayloPreferences) }
            }
    }
}
