package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalValidatorTest {

    @Test
    fun acceptsPlainWholeNumbers() {
        val result = DailyGoalValidator.validate("6000")

        assertEquals(GoalValidationResult.Valid(6_000L), result)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        val result = DailyGoalValidator.validate("  8000  ")

        assertEquals(GoalValidationResult.Valid(8_000L), result)
    }

    @Test
    fun acceptsBoundaryValues() {
        assertEquals(GoalValidationResult.Valid(1_000L), DailyGoalValidator.validate("1000"))
        assertEquals(GoalValidationResult.Valid(100_000L), DailyGoalValidator.validate("100000"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertTrue(DailyGoalValidator.validate("") is GoalValidationResult.Invalid)
        assertTrue(DailyGoalValidator.validate("   ") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsNonNumericInput() {
        val result = DailyGoalValidator.validate("abc")

        assertTrue(result is GoalValidationResult.Invalid)
        assertTrue((result as GoalValidationResult.Invalid).message.isNotBlank())
    }

    @Test
    fun rejectsFractionalNumbers() {
        assertTrue(DailyGoalValidator.validate("6000.5") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsZero() {
        assertTrue(DailyGoalValidator.validate("0") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsNegativeValues() {
        assertTrue(DailyGoalValidator.validate("-500") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsValuesBelowTheMinimum() {
        assertTrue(DailyGoalValidator.validate("999") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsValuesAboveTheMaximum() {
        assertTrue(DailyGoalValidator.validate("100001") is GoalValidationResult.Invalid)
    }

    @Test
    fun rejectsOverflowingNumbers() {
        assertTrue(
            DailyGoalValidator.validate("99999999999999999999") is GoalValidationResult.Invalid,
        )
    }
}
