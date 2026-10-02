package com.waylo.app.ui.walk

import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWalkViewModelTest {

    private class FakeWalkingRepository(
        initialState: WalkingStatus = WalkingStatus(),
    ) : WalkingRepository {
        private val _status = MutableStateFlow(initialState)
        override val status: StateFlow<WalkingStatus> = _status.asStateFlow()

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
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val repository = FakeWalkingRepository()
    private var serviceStarts = 0
    private var currentTime = 1_000_000L

    private fun createViewModel(
        startService: () -> Unit = { serviceStarts += 1 },
    ): ActiveWalkViewModel = ActiveWalkViewModel(
        repository = repository,
        startService = startService,
        now = { currentTime },
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
}
