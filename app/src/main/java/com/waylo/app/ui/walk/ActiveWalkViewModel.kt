package com.waylo.app.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.core.common.WorkoutStatisticsCalculator
import com.waylo.app.data.map.MapLatLng
import com.waylo.app.data.map.WalkMapCameraPolicy
import com.waylo.app.data.map.WalkMapState
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import com.waylo.app.domain.model.WorkoutStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val route: WalkRoute = WalkRoute(),
    val map: WalkMapState = WalkMapState(),
    val statistics: WorkoutStatistics = WorkoutStatistics(),
    val weightKg: Int? = null,
)

class ActiveWalkViewModel(
    private val repository: WalkingRepository,
    private val startService: () -> Unit,
    private val now: () -> Long,
    private val networkStatus: Flow<Boolean> = flowOf(true),
    private val stepState: StateFlow<DailyStepState> = MutableStateFlow(DailyStepState()),
    private val weightKg: Flow<Int?> = flowOf(null),
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private data class StepInputs(
        val stepState: DailyStepState,
        val weightKg: Int?,
        val readiness: WalkReadiness,
    )

    private val scope = stateScope ?: viewModelScope
    private val readiness = MutableStateFlow(WalkReadiness.Ready)
    private val tick = MutableStateFlow(now())
    private val mapState = MutableStateFlow(WalkMapState())

    private val stepInputs = combine(stepState, weightKg, readiness) { steps, weight, current ->
        StepInputs(stepState = steps, weightKg = weight, readiness = current)
    }

    val uiState: StateFlow<ActiveWalkUiState> = combine(
        stepInputs,
        repository.status,
        tick,
        repository.route,
        mapState,
    ) { inputs, status, nowMillis, route, currentMap ->
        ActiveWalkUiState(
            status = status,
            readiness = inputs.readiness,
            elapsedMillis = status.activeMillisAt(nowMillis),
            route = route,
            map = currentMap,
            weightKg = inputs.weightKg,
            statistics = WorkoutStatisticsCalculator.statistics(
                distanceMeters = status.distanceMeters,
                activeMillis = status.activeMillisAt(nowMillis),
                liveMovement = status.state == WalkingState.Active,
                route = route,
                nowMillis = nowMillis,
                weightKg = inputs.weightKg?.toDouble(),
                walkStartStepCount = status.walkStartStepCount,
                currentSensorCount = inputs.stepState.lastSensorCount,
                finalWalkStepCount = status.walkStepCount,
            ),
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
        scope.launch {
            networkStatus.collect { available ->
                mapState.value = WalkMapCameraPolicy.onNetworkChanged(mapState.value, available)
            }
        }
        scope.launch {
            repository.route.collect { route ->
                val latest = route.latestOrNull() ?: return@collect
                mapState.value = WalkMapCameraPolicy.onFix(
                    mapState.value,
                    MapLatLng(latest.latitude, latest.longitude),
                )
            }
        }
        scope.launch {
            repository.status.collect { status ->
                when (status.state) {
                    WalkingState.Starting -> {
                        mapState.value = WalkMapCameraPolicy.onWalkStarting(mapState.value)
                    }

                    WalkingState.Completed -> {
                        mapState.value = WalkMapCameraPolicy.onWalkCompleted(
                            mapState.value,
                            repository.route.value,
                        )
                    }

                    else -> Unit
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

    fun onMapStyleLoaded() {
        mapState.value = WalkMapCameraPolicy.onStyleLoaded(mapState.value)
    }

    fun onMapStyleFailed() {
        mapState.value = WalkMapCameraPolicy.onStyleFailed(mapState.value)
    }

    fun onMapPanGesture() {
        mapState.value = WalkMapCameraPolicy.onMapPan(mapState.value)
    }

    fun onRecenterMap() {
        mapState.value = WalkMapCameraPolicy.onRecenter(mapState.value)
    }

    fun retryMapStyle() {
        mapState.value = WalkMapCameraPolicy.onRetryStyle(mapState.value)
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
            networkStatus: Flow<Boolean> = flowOf(true),
            stepState: StateFlow<DailyStepState> = MutableStateFlow(DailyStepState()),
            weightKg: Flow<Int?> = flowOf(null),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActiveWalkViewModel(
                    repository = repository,
                    startService = startService,
                    now = now,
                    networkStatus = networkStatus,
                    stepState = stepState,
                    weightKg = weightKg,
                )
            }
        }
    }
}
