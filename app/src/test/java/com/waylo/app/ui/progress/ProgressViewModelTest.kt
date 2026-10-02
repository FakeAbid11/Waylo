package com.waylo.app.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressViewModelTest {

    @Test
    fun initialStateReportsNoProgressYet() {
        val state = ProgressViewModel().uiState.value

        assertEquals(1, state.progress.level)
        assertEquals(0, state.progress.xp)
        assertEquals(100, state.progress.xpToNextLevel)
        assertEquals(0, state.progress.xpProgressPercent)
        assertEquals(0, state.progress.currentStreakDays)
        assertEquals(0, state.progress.longestStreakDays)
        assertEquals(0, state.progress.totalSteps)
        assertEquals(0.0, state.progress.totalDistanceKm, 0.0)
        assertEquals(0, state.progress.totalWalkingMinutes)
    }
}
