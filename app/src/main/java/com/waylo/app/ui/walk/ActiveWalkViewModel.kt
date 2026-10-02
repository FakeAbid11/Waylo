package com.waylo.app.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WalkReadiness {
    Ready,
    PermissionNeeded,
    PermissionPermanentlyDenied,
    LocationDisabled,
}

enum class WalkEntryAction {
    None,
    RequestPermission,
}

data class ActiveWalkUiState(
    val status: WalkingStatus = WalkingStatus(),
    val readiness: WalkReadiness = WalkReadiness.Ready,
    val elapsedMillis: Long = 0L,
)

class ActiveWalkViewModel(
    private val repository: WalkingRepository,
    private val startService: () -> Unit,
    private val now: () -> Long,
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope = stateScope ?: viewModelScope
    private val readiness = MutableStateFlow(WalkReadiness.Ready)
    private val tick = MutableStateFlow(now())

    val uiState: StateFlow<ActiveWalkUiState> = combine(
        repository.status,
        readiness,
        tick,
    ) { status, currentReadiness, nowMillis ->
        ActiveWalkUiState(
            status = status,
            readiness = currentReadiness,
            elapsedMillis = status.activeMillisAt(nowMillis),
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ActiveWalkUiState(),
    )

    init {
        scope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                if (uiState.value.status.state == WalkingState.Active) {
                    tick.value = now()
                }
            }
        }
    }

    fun onScreenResumed(permission: PermissionState, locationEnabled: Boolean): WalkEntryAction {
        val nextReadiness = when {
            permission == PermissionState.PermanentlyDenied -> WalkReadiness.PermissionPermanentlyDenied
            permission == PermissionState.Granted || permission == PermissionState.Unsupported -> {
                if (locationEnabled) WalkReadiness.Ready else WalkReadiness.LocationDisabled
            }
            else -> WalkReadiness.PermissionNeeded
        }
        readiness.value = nextReadiness
        val state = repository.status.value.state
        return when {
            nextReadiness == WalkReadiness.PermissionNeeded &&
                permission == PermissionState.NotRequested -> WalkEntryAction.RequestPermission

            nextReadiness != WalkReadiness.Ready -> WalkEntryAction.None

            state.isOngoing -> {
                startServiceSafely()
                WalkEntryAction.None
            }

            state == WalkingState.Idle -> {
                repository.startWalk()
                startServiceSafely()
                WalkEntryAction.None
            }

            else -> WalkEntryAction.None
        }
    }

    fun pauseWalk() {
        repository.pauseWalk()
    }

    fun resumeWalk() {
        if (readiness.value == WalkReadiness.Ready) {
            repository.resumeWalk()
        }
    }

    fun finishWalk() {
        repository.stopWalk()
    }

    fun retryWalk() {
        if (readiness.value != WalkReadiness.Ready) return
        repository.retryTracking()
        startServiceSafely()
    }

    fun finishCompleted() {
        repository.dismissCompleted()
    }

    private fun startServiceSafely() {
        try {
            startService()
        } catch (exception: Exception) {
            repository.reportError("Walk tracking couldn't start.")
        }
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L

        fun factory(
            repository: WalkingRepository,
            startService: () -> Unit,
            now: () -> Long,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActiveWalkViewModel(
                    repository = repository,
                    startService = startService,
                    now = now,
                )
            }
        }
    }
}
