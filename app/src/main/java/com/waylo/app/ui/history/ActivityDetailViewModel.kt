package com.waylo.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.common.WorkoutStatisticsCalculator
import com.waylo.app.core.util.WayloDateFormatter
import com.waylo.app.data.map.WalkMapCameraPolicy
import com.waylo.app.data.map.WalkMapState
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingSession
import com.waylo.app.domain.model.WorkoutStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed interface ActivityDetailUiState {
    data object Loading : ActivityDetailUiState
    data object NotFound : ActivityDetailUiState
    data object Error : ActivityDetailUiState

    data class Loaded(
        val session: WalkingSession,
        val route: WalkRoute,
        val routeLoaded: Boolean,
        val routeUnavailable: Boolean,
        val statistics: WorkoutStatistics,
        val map: WalkMapState,
        val weightKg: Int?,
        val dateLabel: String,
        val startTimeLabel: String,
        val endTimeLabel: String?,
    ) : ActivityDetailUiState
}

class ActivityDetailViewModel(
    private val repository: WalkingRepository,
    private val activityId: Long,
    private val now: () -> Long,
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    private val networkStatus: Flow<Boolean> = flowOf(true),
    private val weightKg: Flow<Int?> = flowOf(null),
    private val onDeleted: () -> Unit = {},
    stateScope: CoroutineScope? = null,
) : ViewModel() {

    private sealed interface RoutePayload {
        data object NotLoaded : RoutePayload
        data class Ready(val route: WalkRoute, val failed: Boolean) : RoutePayload
    }

    private val scope = stateScope ?: viewModelScope
    private val mapState = MutableStateFlow(WalkMapState())
    private val routePayload = MutableStateFlow<RoutePayload>(RoutePayload.NotLoaded)
    private val _uiState = MutableStateFlow<ActivityDetailUiState>(ActivityDetailUiState.Loading)

    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private var session: WalkingSession? = null
    private var weight: Int? = null
    private var seenSession = false
    private var sessionFailed = false
    private var routeRequested = false
    private var sessionJob: Job? = null

    init {
        observeSession()
        scope.launch {
            weightKg.collect { value ->
                weight = value
                rebuild()
            }
        }
        scope.launch {
            networkStatus.collect { available ->
                mapState.value = WalkMapCameraPolicy.onNetworkChanged(mapState.value, available)
            }
        }
        scope.launch {
            mapState.collect { rebuild() }
        }
    }

    fun retry() {
        sessionFailed = false
        seenSession = false
        session = null
        routeRequested = false
        routePayload.value = RoutePayload.NotLoaded
        _uiState.value = ActivityDetailUiState.Loading
        observeSession()
    }

    fun delete() {
        scope.launch {
            if (repository.deleteActivity(activityId)) {
                onDeleted()
            }
        }
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

    fun retryMapStyle() {
        mapState.value = WalkMapCameraPolicy.onRetryStyle(mapState.value)
    }

    private fun observeSession() {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            try {
                repository.observeSession(activityId).collect { row ->
                    seenSession = true
                    session = row
                    if (row != null && !routeRequested) {
                        loadRoute()
                    }
                    rebuild()
                }
            } catch (t: Throwable) {
                sessionFailed = true
                rebuild()
            }
        }
    }

    private suspend fun loadRoute() {
        routeRequested = true
        routePayload.value = try {
            RoutePayload.Ready(repository.routeForSession(activityId), failed = false)
        } catch (t: Throwable) {
            RoutePayload.Ready(WalkRoute(), failed = true)
        }
        val payload = routePayload.value as RoutePayload.Ready
        mapState.value = WalkMapCameraPolicy.onWalkCompleted(mapState.value, payload.route)
    }

    private fun rebuild() {
        _uiState.value = when {
            sessionFailed -> ActivityDetailUiState.Error
            !seenSession -> ActivityDetailUiState.Loading
            session == null -> ActivityDetailUiState.NotFound
            else -> buildLoaded(session!!)
        }
    }

    private fun buildLoaded(current: WalkingSession): ActivityDetailUiState.Loaded {
        val payload = routePayload.value
        val ready = payload as? RoutePayload.Ready
        val zone = zoneId()
        return ActivityDetailUiState.Loaded(
            session = current,
            route = ready?.route ?: WalkRoute(),
            routeLoaded = ready != null,
            routeUnavailable = ready != null && (ready.failed || ready.route.isEmpty),
            statistics = WorkoutStatisticsCalculator.statistics(
                distanceMeters = current.distanceMeters,
                activeMillis = current.activeMillis,
                liveMovement = false,
                route = WalkRoute(),
                nowMillis = now(),
                weightKg = weight?.toDouble(),
                walkStartStepCount = current.walkStartStepCount,
                currentSensorCount = null,
                finalWalkStepCount = current.walkStepCount,
            ),
            map = mapState.value,
            weightKg = weight,
            dateLabel = WayloDateFormatter.fullDate(
                WayloDateFormatter.localDate(current.startMillis, zone),
            ),
            startTimeLabel = WayloDateFormatter.timeOfDay(current.startMillis, zone),
            endTimeLabel = current.updatedMillis
                .takeIf { it > current.startMillis }
                ?.let { WayloDateFormatter.timeOfDay(it, zone) },
        )
    }

    companion object {
        fun factory(
            repository: WalkingRepository,
            activityId: Long,
            now: () -> Long,
            zoneId: () -> ZoneId = { ZoneId.systemDefault() },
            networkStatus: Flow<Boolean> = flowOf(true),
            weightKg: Flow<Int?> = flowOf(null),
            onDeleted: () -> Unit = {},
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ActivityDetailViewModel(
                    repository = repository,
                    activityId = activityId,
                    now = now,
                    zoneId = zoneId,
                    networkStatus = networkStatus,
                    weightKg = weightKg,
                    onDeleted = onDeleted,
                )
            }
        }
    }
}
