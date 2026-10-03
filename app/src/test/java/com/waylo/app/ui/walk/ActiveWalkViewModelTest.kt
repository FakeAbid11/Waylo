package com.waylo.app.ui.walk

import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.map.MapCameraCommand
import com.waylo.app.data.map.MapLatLng
import com.waylo.app.data.map.WalkMapLoadState
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWalkViewModelTest {

    private class FakeWalkingRepository(
        initialState: WalkingStatus = WalkingStatus(),
    ) : WalkingRepository {
        private val _status = MutableStateFlow(initialState)
        override val status: StateFlow<WalkingStatus> = _status.asStateFlow()

        private val _route = MutableStateFlow(WalkRoute())
        override val route: StateFlow<WalkRoute> = _route.asStateFlow()

        var startCalls = 0
        var pauseCalls = 0
        var resumeCalls = 0
        var stopCalls = 0
        var retryCalls = 0
        var dismissCalls = 0
        var reportErrorCalls = 0

        override fun startWalk() {
            startCalls += 1
        }

        override fun pauseWalk() {
            pauseCalls += 1
        }

        override fun resumeWalk() {
            resumeCalls += 1
        }

        override fun stopWalk() {
            stopCalls += 1
        }

        override fun retryTracking() {
            retryCalls += 1
        }

        override fun dismissCompleted() {
            dismissCalls += 1
        }

        override fun attachService() = Unit
        override fun detachService() = Unit

        override fun reportError(message: String) {
            reportErrorCalls += 1
        }

        fun emit(status: WalkingStatus) {
            _status.value = status
        }

        fun emitRoute(route: WalkRoute) {
            _route.value = route
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val repository = FakeWalkingRepository()
    private var serviceStarts = 0
    private var currentTime = 1_000_000L

    private fun createViewModel(
        startService: () -> Unit = { serviceStarts += 1 },
        networkStatus: Flow<Boolean> = flowOf(true),
    ): ActiveWalkViewModel = ActiveWalkViewModel(
        repository = repository,
        startService = startService,
        now = { currentTime },
        networkStatus = networkStatus,
        stateScope = testScope,
    )

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun freshScreenIsIdleAndReady() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(WalkingState.Idle, state.status.state)
        assertEquals(WalkReadiness.Ready, state.readiness)
        assertEquals(0L, state.elapsedMillis)
    }

    @Test
    fun firstGrantStartsWalkAndService() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(1, repository.startCalls)
        assertEquals(1, serviceStarts)
    }

    @Test
    fun notRequestedPermissionAsksFirst() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.NotRequested, locationEnabled = true)

        assertEquals(WalkEntryAction.RequestPermission, action)
        assertEquals(0, repository.startCalls)
        assertEquals(0, serviceStarts)
        assertEquals(WalkReadiness.PermissionNeeded, viewModel.uiState.value.readiness)
    }

    @Test
    fun deniedPermissionShowsTheCardWithoutStarting() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Denied, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(WalkReadiness.PermissionNeeded, viewModel.uiState.value.readiness)
        assertEquals(0, repository.startCalls)
        assertEquals(0, serviceStarts)
    }

    @Test
    fun permanentlyDeniedPermissionMapsToBlockedReadiness() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.PermanentlyDenied, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(WalkReadiness.PermissionPermanentlyDenied, viewModel.uiState.value.readiness)
        assertEquals(0, repository.startCalls)
    }

    @Test
    fun disabledLocationServicesBlockStart() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = false)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(WalkReadiness.LocationDisabled, viewModel.uiState.value.readiness)
        assertEquals(0, repository.startCalls)
        assertEquals(0, serviceStarts)
    }

    @Test
    fun unsupportedPermissionStillCountsAsReady() {
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Unsupported, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(WalkReadiness.Ready, viewModel.uiState.value.readiness)
        assertEquals(1, repository.startCalls)
    }

    @Test
    fun returningToAnOngoingWalkOnlyReattachesTheService() {
        repository.emit(WalkingStatus(state = WalkingState.Active))
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(1, serviceStarts)
        assertEquals(0, repository.startCalls)
    }

    @Test
    fun errorStateIsNeverAutoRetriedOnResume() {
        repository.emit(WalkingStatus(state = WalkingState.Error, errorMessage = "boom"))
        val viewModel = createViewModel()

        val action = viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = true)

        assertEquals(WalkEntryAction.None, action)
        assertEquals(0, repository.retryCalls)
        assertEquals(0, serviceStarts)
    }

    @Test
    fun retryWalkRetriesAndRestartsTheServiceWhenReady() {
        repository.emit(WalkingStatus(state = WalkingState.Error, errorMessage = "boom"))
        val viewModel = createViewModel()
        viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = true)

        viewModel.retryWalk()

        assertEquals(1, repository.retryCalls)
        assertEquals(1, serviceStarts)
    }

    @Test
    fun retryWalkIsIgnoredWhenNotReady() {
        val viewModel = createViewModel()
        viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = false)

        viewModel.retryWalk()

        assertEquals(0, repository.retryCalls)
        assertEquals(0, serviceStarts)
    }

    @Test
    fun timerAddsTheRunningSegmentToFoldedTime() {
        currentTime = 2_000_000L
        val viewModel = createViewModel()
        repository.emit(
            WalkingStatus(
                state = WalkingState.Active,
                activeMillis = 60_000L,
                activeSegmentStartMillis = 1_700_000L,
            ),
        )

        assertEquals(360_000L, viewModel.uiState.value.elapsedMillis)
    }

    @Test
    fun timerFreezesWhilePaused() {
        currentTime = 2_000_000L
        val viewModel = createViewModel()
        repository.emit(
            WalkingStatus(
                state = WalkingState.Paused,
                activeMillis = 300_000L,
                activeSegmentStartMillis = null,
            ),
        )

        assertEquals(300_000L, viewModel.uiState.value.elapsedMillis)
    }

    @Test
    fun pauseResumeAndFinishDelegateToRepository() {
        val viewModel = createViewModel()

        viewModel.pauseWalk()
        viewModel.resumeWalk()
        viewModel.finishWalk()
        viewModel.finishCompleted()

        assertEquals(1, repository.pauseCalls)
        assertEquals(1, repository.resumeCalls)
        assertEquals(1, repository.stopCalls)
        assertEquals(1, repository.dismissCalls)
    }

    @Test
    fun serviceStartFailureReportsAnError() {
        var reportErrors = 0
        val failingRepository = object : WalkingRepository by FakeWalkingRepository() {
            override fun reportError(message: String) {
                reportErrors += 1
            }
        }
        val viewModel = ActiveWalkViewModel(
            repository = failingRepository,
            startService = { throw IllegalStateException("nope") },
            now = { currentTime },
            stateScope = testScope,
        )

        viewModel.onScreenResumed(PermissionState.Granted, locationEnabled = true)

        assertEquals(1, reportErrors)
    }

    @Test
    fun mapLoadFailuresRecoverThroughRetry() {
        val viewModel = createViewModel()
        assertEquals(WalkMapLoadState.Loading, viewModel.uiState.value.map.loadState)

        viewModel.onMapStyleFailed()
        assertEquals(WalkMapLoadState.StyleError, viewModel.uiState.value.map.loadState)

        viewModel.retryMapStyle()
        val retried = viewModel.uiState.value.map
        assertEquals(WalkMapLoadState.Loading, retried.loadState)
        assertEquals(1L, retried.styleGeneration)

        viewModel.onMapStyleLoaded()
        assertEquals(WalkMapLoadState.Ready, viewModel.uiState.value.map.loadState)
    }

    @Test
    fun losingTheNetworkHidesTheMapUntilItReturns() {
        val network = MutableStateFlow(true)
        val viewModel = createViewModel(networkStatus = network)
        viewModel.onMapStyleLoaded()
        assertEquals(WalkMapLoadState.Ready, viewModel.uiState.value.map.loadState)

        network.value = false
        assertEquals(WalkMapLoadState.Unavailable, viewModel.uiState.value.map.loadState)

        network.value = true
        assertEquals(WalkMapLoadState.Ready, viewModel.uiState.value.map.loadState)
        assertEquals(0L, viewModel.uiState.value.map.styleGeneration)
    }

    @Test
    fun routeProgressIsExposedAndTheCameraFollowsTheFirstFix() {
        val viewModel = createViewModel()
        val expected = WalkRoute().withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))

        repository.emitRoute(expected)

        assertEquals(expected, viewModel.uiState.value.route)
        val command = viewModel.uiState.value.map.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(MapLatLng(52.0, 13.0), command.target)
        assertTrue(viewModel.uiState.value.map.followEnabled)
    }

    @Test
    fun panningStopsTheFollowAndRecenteringResumesIt() {
        val viewModel = createViewModel()
        repository.emitRoute(WalkRoute().withPoint(point(0, 52.0, 13.0)))
        val followCommand = viewModel.uiState.value.map.cameraCommand

        viewModel.onMapPanGesture()
        assertFalse(viewModel.uiState.value.map.followEnabled)

        repository.emitRoute(
            WalkRoute()
                .withPoint(point(0, 52.0, 13.0))
                .withPoint(point(1, 52.01, 13.0)),
        )
        assertSame(followCommand, viewModel.uiState.value.map.cameraCommand)

        viewModel.onRecenterMap()
        val recentered = viewModel.uiState.value.map
        assertTrue(recentered.followEnabled)
        val command = recentered.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(MapLatLng(52.01, 13.0), command.target)
    }

    @Test
    fun finishingTheWalkFitsTheWholeRouteExactlyOnce() {
        val viewModel = createViewModel()
        repository.emitRoute(
            WalkRoute()
                .withPoint(point(0, 52.0, 13.0))
                .withPoint(point(1, 52.02, 13.03)),
        )

        repository.emit(WalkingStatus(state = WalkingState.Completed, sessionId = 1L))
        val command = viewModel.uiState.value.map.cameraCommand as MapCameraCommand.FitBounds
        assertEquals(52.0, command.bounds.minLatitude, 0.0)
        assertEquals(52.02, command.bounds.maxLatitude, 0.0)
        assertFalse(viewModel.uiState.value.map.followEnabled)

        repository.emit(WalkingStatus(state = WalkingState.Idle))
        repository.emit(WalkingStatus(state = WalkingState.Completed, sessionId = 1L))
        val again = viewModel.uiState.value.map.cameraCommand as MapCameraCommand.FitBounds
        assertEquals(command.id, again.id)
    }

    private fun point(
        sequence: Int,
        latitude: Double,
        longitude: Double,
    ): WalkingLocationPoint = WalkingLocationPoint(
        sessionId = 1L,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = 1_000L + sequence,
    )
}
