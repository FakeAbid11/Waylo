package com.waylo.app.domain.model

import com.waylo.app.core.common.fractionToPercent
import com.waylo.app.core.common.ratioToFraction

data class DailyGoal(
    val targetSteps: Int,
    val completedSteps: Int,
) {
    val progress: Float
        get() = ratioToFraction(completedSteps.toFloat(), targetSteps.toFloat())

    val progressPercent: Int
        get() = fractionToPercent(progress)

    val isCompleted: Boolean
        get() = targetSteps > 0 && completedSteps >= targetSteps

    companion object {
        const val DEFAULT_TARGET_STEPS = 6_000

        fun empty(targetSteps: Int = DEFAULT_TARGET_STEPS): DailyGoal {
            return DailyGoal(targetSteps = targetSteps, completedSteps = 0)
        }
    }
}
