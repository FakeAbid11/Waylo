package com.waylo.app.ui.home

import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.step.StepRepository
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.StepStatus
import com.waylo.app.domain.model.WalkRoute
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private class FakeStepRepository(
        initialState: DailyStepState = DailyStepState(),
    ) : StepRepository {
        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<DailyStepState> = _state.asStateFlow()

        var startCalls = 0
        var stopCalls = 0
        var refreshCalls = 0
        var savedGoal: Long? = null

        override fun start() {
            startCalls += 1
        }

        override fun refresh() {
            refreshCalls += 1
        }

        override fun stop() {
            stopCalls += 1
        }

        override suspend fun setDailyGoal(goal: Long) {
            savedGoal = goal
        }

        fun emit(stepState: DailyStepState) {
            _state.value = stepState
        }
    }

    private class FakeWalkingRepository(
        initialState: WalkingStatus = WalkingStatus(),
    ) : WalkingRepository {
        private val _status = MutableStateFlow(initialState)
        override val status: StateFlow<WalkingStatus> = _status.asStateFlow()
        override val route: StateFlow<WalkRoute> = MutableStateFlow(WalkRoute()).asStateFlow()

        override fun startWalk() = Unit
        override fun pauseWalk() = Unit
        override fun resumeWalk() = Unit
        override fun stopWalk() = Unit
        override fun retryTracking() = Unit
        override fun dismissCompleted() = Unit
        override fun attachService() = Unit
        override fun detachService() = Unit
        override fun reportError(message: String) = Unit

        fun emit(status: WalkingStatus) {
            _status.value = status
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val repository = FakeStepRepository()
    private val walkingRepository = FakeWalkingRepository()
    private val viewModel = HomeViewModel(repository, walkingRepository, testScope)

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun initialStateShowsAnEmptyDay() {
        val state = viewModel.uiState.value

        assertEquals("Good evening", state.greeting)
        assertEquals(6_000, state.goal.targetSteps)
        assertEquals(0, state.goal.completedSteps)
        assertEquals(0f, state.goal.progress, 0f)
        assertEquals(0, state.goal.progressPercent)
        assertEquals(1, state.progress.level)
        assertEquals(0, state.progress.xp)
        assertEquals(0, state.progress.currentStreakDays)
        assertEquals(0, state.todaySteps)
        assertEquals(0.0, state.todayDistanceKm, 0.0)
        assertEquals(0, state.todayWalkingMinutes)
        assertEquals(WalkingState.Idle, state.walkState)
        assertTrue(state.isStartWalkAvailable)
        assertEquals("Start Walk", state.startWalkLabel)
    }

    @Test
    fun startsRepositoryOnCreation() {
        assertEquals(1, repository.startCalls)
    }

    @Test
    fun mirrorsRepositoryStepState() {
        repository.emit(
            DailyStepState(
                steps = 3_482L,
                goal = 8_000L,
                sensorAvailable = true,
                permissionState = PermissionState.Granted,
                isTracking = true,
                isLoaded = true,
            ),
        )

        val state = viewModel.uiState.value

        assertEquals(3_482, state.todaySteps)
        assertEquals(8_000, state.goal.targetSteps)
        assertEquals(3_482, state.goal.completedSteps)
        assertEquals(StepStatus.Active, state.stepState.status)
    }

    @Test
    fun ongoingWalkRelabelsTheStartButton() {
        walkingRepository.emit(WalkingStatus(state = WalkingState.Active))

        val state = viewModel.uiState.value

        assertEquals(WalkingState.Active, state.walkState)
        assertTrue(state.isStartWalkAvailable)
        assertEquals("Return to Walk", state.startWalkLabel)
    }

    @Test
    fun stoppingWalkHidesTheStartButton() {
        walkingRepository.emit(WalkingStatus(state = WalkingState.Stopping))

        val state = viewModel.uiState.value

        assertFalse(state.isStartWalkAvailable)
    }

    @Test
    fun completedWalkRestoresTheStartLabel() {
        walkingRepository.emit(WalkingStatus(state = WalkingState.Completed))

        val state = viewModel.uiState.value

        assertTrue(state.isStartWalkAvailable)
        assertEquals("Start Walk", state.startWalkLabel)
    }

    @Test
    fun refreshDelegatesToRepository() {
        viewModel.refresh()

        assertEquals(1, repository.refreshCalls)
    }
}
