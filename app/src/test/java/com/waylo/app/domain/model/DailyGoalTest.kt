package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalTest {

    @Test
    fun emptyGoalHasNoProgress() {
        val goal = DailyGoal.empty()

        assertEquals(DailyGoal.DEFAULT_TARGET_STEPS, goal.targetSteps)
        assertEquals(0, goal.completedSteps)
        assertEquals(0f, goal.progress, 0f)
        assertEquals(0, goal.progressPercent)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun partialGoalReportsPartialProgress() {
        val goal = DailyGoal(targetSteps = 6_000, completedSteps = 3_000)

        assertEquals(0.5f, goal.progress, 0.001f)
        assertEquals(50, goal.progressPercent)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun completedGoalReportsFullProgress() {
        val goal = DailyGoal(targetSteps = 6_000, completedSteps = 6_000)

        assertEquals(1f, goal.progress, 0f)
        assertEquals(100, goal.progressPercent)
        assertTrue(goal.isCompleted)
    }

    @Test
    fun progressIsCappedWhenTargetIsExceeded() {
        val goal = DailyGoal(targetSteps = 6_000, completedSteps = 9_000)

        assertEquals(1f, goal.progress, 0f)
        assertEquals(100, goal.progressPercent)
        assertTrue(goal.isCompleted)
    }

    @Test
    fun zeroTargetHasNoProgress() {
        val goal = DailyGoal(targetSteps = 0, completedSteps = 100)

        assertEquals(0f, goal.progress, 0f)
        assertEquals(0, goal.progressPercent)
        assertFalse(goal.isCompleted)
    }
}
