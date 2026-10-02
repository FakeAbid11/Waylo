package com.waylo.app.ui.home

import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.step.StepRepository
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.StepStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val repository = FakeStepRepository()
    private val viewModel = HomeViewModel(repository, testScope)

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
        assertFalse(state.isStartWalkAvailable)
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
    fun refreshDelegatesToRepository() {
        viewModel.refresh()

        assertEquals(1, repository.refreshCalls)
    }
}
