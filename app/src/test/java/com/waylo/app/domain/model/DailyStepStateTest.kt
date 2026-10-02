package com.waylo.app.domain.model

import com.waylo.app.core.permissions.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyStepStateTest {

    @Test
    fun statusIsLoadingUntilTheRepositoryLoads() {
        assertEquals(StepStatus.Loading, DailyStepState().status)
    }

    @Test
    fun grantedPermissionIsActive() {
        val state = loadedState(permissionState = PermissionState.Granted)

        assertEquals(StepStatus.Active, state.status)
    }

    @Test
    fun unsupportedPermissionIsActiveOnOlderDevices() {
        val state = loadedState(permissionState = PermissionState.Unsupported)

        assertEquals(StepStatus.Active, state.status)
    }

    @Test
    fun missingPermissionNeedsAction() {
        assertEquals(
            StepStatus.PermissionNeeded,
            loadedState(permissionState = PermissionState.NotRequested).status,
        )
        assertEquals(
            StepStatus.PermissionNeeded,
            loadedState(permissionState = PermissionState.Denied).status,
        )
        assertEquals(
            StepStatus.PermissionNeeded,
            loadedState(permissionState = PermissionState.PermanentlyDenied).status,
        )
    }

    @Test
    fun missingSensorBeatsPermissionState() {
        val state = loadedState(
            sensorAvailable = false,
            permissionState = PermissionState.Granted,
        )

        assertEquals(StepStatus.SensorUnavailable, state.status)
    }

    @Test
    fun goalCompletenessTracksStepsAgainstGoal() {
        assertFalse(loadedState(steps = 5_999L).isGoalComplete)
        assertTrue(loadedState(steps = 6_000L).isGoalComplete)
        assertTrue(loadedState(steps = 8_000L).isGoalComplete)
    }

    @Test
    fun asDailyGoalDerivesProgress() {
        val goal = loadedState(steps = 3_000L).asDailyGoal()

        assertEquals(6_000, goal.targetSteps)
        assertEquals(3_000, goal.completedSteps)
        assertEquals(0.5f, goal.progress, 0f)
        assertEquals(50, goal.progressPercent)
    }

    @Test
    fun progressVisuallyCapsAtFullButStoredStepsStayUncapped() {
        val state = loadedState(steps = 8_000L)
        val goal = state.asDailyGoal()

        assertEquals(1f, goal.progress, 0f)
        assertEquals(100, goal.progressPercent)
        assertEquals(8_000, goal.completedSteps)
        assertTrue(state.isGoalComplete)
    }

    @Test
    fun hugeStepCountsCoerceToDisplayableValues() {
        val state = loadedState(steps = Long.MAX_VALUE, goal = Long.MAX_VALUE)

        val goal = state.asDailyGoal()

        assertEquals(Int.MAX_VALUE, goal.completedSteps)
        assertEquals(Int.MAX_VALUE, goal.targetSteps)
    }

    private fun loadedState(
        steps: Long = 0,
        goal: Long = 6_000,
        sensorAvailable: Boolean = true,
        permissionState: PermissionState = PermissionState.Granted,
    ) = DailyStepState(
        steps = steps,
        goal = goal,
        sensorAvailable = sensorAvailable,
        permissionState = permissionState,
        isTracking = sensorAvailable,
        isLoaded = true,
    )
}
