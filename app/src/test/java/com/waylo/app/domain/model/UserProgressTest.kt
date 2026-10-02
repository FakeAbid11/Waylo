package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProgressTest {

    @Test
    fun emptyProgressStartsAtLevelOne() {
        val progress = UserProgress.empty()

        assertEquals(1, progress.level)
        assertEquals(0, progress.xp)
        assertEquals(100, progress.xpToNextLevel)
        assertEquals(0f, progress.xpProgress, 0f)
        assertEquals(0, progress.xpProgressPercent)
        assertEquals(0, progress.currentStreakDays)
        assertEquals(0, progress.longestStreakDays)
        assertEquals(0, progress.totalSteps)
        assertEquals(0.0, progress.totalDistanceKm, 0.0)
        assertEquals(0, progress.totalWalkingMinutes)
    }

    @Test
    fun xpProgressIsFractionOfRequiredXp() {
        val progress = UserProgress.empty().copy(xp = 25, xpToNextLevel = 100)

        assertEquals(0.25f, progress.xpProgress, 0.001f)
        assertEquals(25, progress.xpProgressPercent)
    }

    @Test
    fun xpProgressIsCappedAtOneHundredPercent() {
        val progress = UserProgress.empty().copy(xp = 150, xpToNextLevel = 100)

        assertEquals(1f, progress.xpProgress, 0f)
        assertEquals(100, progress.xpProgressPercent)
    }

    @Test
    fun zeroRequiredXpHasNoProgress() {
        val progress = UserProgress.empty().copy(xp = 10, xpToNextLevel = 0)

        assertEquals(0f, progress.xpProgress, 0f)
        assertEquals(0, progress.xpProgressPercent)
    }
}
