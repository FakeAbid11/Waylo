package com.waylo.app.domain.model

sealed interface GoalValidationResult {
    data class Valid(val steps: Long) : GoalValidationResult
    data class Invalid(val message: String) : GoalValidationResult
}

object DailyGoalValidator {

    const val MIN_STEPS = 1_000L
    const val MAX_STEPS = 100_000L
    val DEFAULT_STEPS: Long = DailyGoal.DEFAULT_TARGET_STEPS.toLong()

    fun validate(input: String): GoalValidationResult {
        val text = input.trim()
        if (text.isEmpty()) return GoalValidationResult.Invalid(EMPTY_MESSAGE)
        val steps = text.toLongOrNull() ?: return GoalValidationResult.Invalid(NOT_A_NUMBER_MESSAGE)
        if (steps < MIN_STEPS) return GoalValidationResult.Invalid(TOO_SMALL_MESSAGE)
        if (steps > MAX_STEPS) return GoalValidationResult.Invalid(TOO_LARGE_MESSAGE)
        return GoalValidationResult.Valid(steps)
    }

    private const val EMPTY_MESSAGE = "Enter a step goal"
    private const val NOT_A_NUMBER_MESSAGE = "Use whole numbers only"
    private const val TOO_SMALL_MESSAGE = "Enter at least 1,000 steps"
    private const val TOO_LARGE_MESSAGE = "Enter at most 100,000 steps"
}
