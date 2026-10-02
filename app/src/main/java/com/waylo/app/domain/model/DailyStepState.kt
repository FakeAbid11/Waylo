package com.waylo.app.domain.model

import com.waylo.app.core.permissions.PermissionState

enum class StepStatus {
    Loading,
    PermissionNeeded,
    SensorUnavailable,
    Active,
}

data class DailyStepState(
    val steps: Long = 0,
    val goal: Long = DailyGoalValidator.DEFAULT_STEPS,
    val sensorAvailable: Boolean = false,
    val permissionState: PermissionState = PermissionState.NotRequested,
    val isTracking: Boolean = false,
    val isLoaded: Boolean = false,
) {
    val status: StepStatus
        get() = when {
            !isLoaded -> StepStatus.Loading
            !sensorAvailable -> StepStatus.SensorUnavailable
            permissionState == PermissionState.Granted ||
                permissionState == PermissionState.Unsupported -> StepStatus.Active
            else -> StepStatus.PermissionNeeded
        }

    val isGoalComplete: Boolean
        get() = goal > 0 && steps >= goal

    fun asDailyGoal(): DailyGoal = DailyGoal(
        targetSteps = goal.toDisplaySteps(),
        completedSteps = steps.toDisplaySteps(),
    )

    private fun Long.toDisplaySteps(): Int = coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}
