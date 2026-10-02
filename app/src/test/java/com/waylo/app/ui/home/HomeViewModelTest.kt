package com.waylo.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun initialStateShowsAnEmptyDay() {
        val state = HomeViewModel().uiState.value

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
}
