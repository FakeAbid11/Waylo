package com.waylo.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCalculatorTest {

    @Test
    fun requiredXpForLevelFollowsTheHundredTimesLevelSquaredCurve() {
        assertEquals(100, LevelCalculator.requiredXpForLevel(1))
        assertEquals(400, LevelCalculator.requiredXpForLevel(2))
        assertEquals(900, LevelCalculator.requiredXpForLevel(3))
    }

    @Test
    fun requiredXpNeverFallsBelowTheStartingThreshold() {
        assertEquals(100, LevelCalculator.requiredXpForLevel(0))
        assertEquals(100, LevelCalculator.requiredXpForLevel(-5))
    }

    @Test
    fun totalXpToReachALevelIsTheSumOfTheCurve() {
        assertEquals(0, LevelCalculator.totalXpForLevel(1))
        assertEquals(100, LevelCalculator.totalXpForLevel(2))
        assertEquals(500, LevelCalculator.totalXpForLevel(3))
        assertEquals(1_400, LevelCalculator.totalXpForLevel(4))
        assertEquals(100, LevelCalculator.totalXpForLevel(2))
    }

    @Test
    fun levelStartsAtOneAndAdvancesOnExactThresholds() {
        assertEquals(1, LevelCalculator.levelForTotalXp(0))
        assertEquals(1, LevelCalculator.levelForTotalXp(99))
        assertEquals(2, LevelCalculator.levelForTotalXp(100))
        assertEquals(2, LevelCalculator.levelForTotalXp(499))
        assertEquals(3, LevelCalculator.levelForTotalXp(500))
        assertEquals(4, LevelCalculator.levelForTotalXp(1_400))
    }

    @Test
    fun negativeXpNeverLowersTheLevelBelowOne() {
        assertEquals(1, LevelCalculator.levelForTotalXp(-1))
        assertEquals(1, LevelCalculator.levelForTotalXp(Int.MIN_VALUE))
    }

    @Test
    fun levelIsMonotonicInTotalXp() {
        var previous = 1
        var total = 0
        while (total < 20_000) {
            val level = LevelCalculator.levelForTotalXp(total)
            assertTrue(level >= previous)
            previous = level
            total += 37
        }
        assertTrue(previous >= 1)
    }

    @Test
    fun zeroXpStartsAtLevelOneWithAFullFirstBar() {
        val state = LevelCalculator.stateFor(0)

        assertEquals(1, state.level)
        assertEquals(0, state.totalXp)
        assertEquals(0, state.currentLevelXp)
        assertEquals(100, state.nextLevelXp)
        assertEquals(0, state.xpIntoCurrentLevel)
        assertEquals(100, state.xpRemaining)
        assertEquals(0f, state.progressFraction, 0f)
    }

    @Test
    fun exactThresholdMovesIntoTheNextLevelWithEmptyProgress() {
        val state = LevelCalculator.stateFor(100)

        assertEquals(2, state.level)
        assertEquals(100, state.currentLevelXp)
        assertEquals(500, state.nextLevelXp)
        assertEquals(0, state.xpIntoCurrentLevel)
        assertEquals(400, state.xpRemaining)
        assertEquals(0f, state.progressFraction, 0f)
    }

    @Test
    fun progressWithinALevelIsAFractionOfThatLevelsRequirement() {
        val state = LevelCalculator.stateFor(250)

        assertEquals(2, state.level)
        assertEquals(150, state.xpIntoCurrentLevel)
        assertEquals(250, state.xpRemaining)
        assertEquals(0.375f, state.progressFraction, 0.0001f)
        assertEquals(state.nextLevelXp - state.currentLevelXp, state.xpIntoCurrentLevel + state.xpRemaining)
    }

    @Test
    fun hugeXpKeepsEveryValueValidAndBounded() {
        val state = LevelCalculator.stateFor(Int.MAX_VALUE)

        assertTrue(state.level >= 1)
        assertTrue(state.level <= LevelCalculator.MAX_LEVEL)
        assertTrue(state.xpIntoCurrentLevel >= 0)
        assertTrue(state.xpRemaining >= 0)
        assertTrue(state.progressFraction == state.progressFraction)
        assertTrue(state.progressFraction >= 0f)
        assertTrue(state.progressFraction <= 1f)
        assertTrue(state.xpIntoCurrentLevel + state.xpRemaining >= state.nextLevelXp - state.currentLevelXp - 1)
    }

    @Test
    fun progressFractionNeverExceedsOneForAnySampledTotal() {
        var total = 0
        while (total <= 100_000) {
            val state = LevelCalculator.stateFor(total)
            assertTrue(state.progressFraction >= 0f)
            assertTrue(state.progressFraction <= 1f)
            assertTrue(state.xpIntoCurrentLevel <= state.nextLevelXp - state.currentLevelXp)
            total += 13
        }
    }

    @Test
    fun stateTotalXpAlwaysMirrorsTheInputAfterClamping() {
        assertEquals(0, LevelCalculator.stateFor(-100).totalXp)
        assertEquals(1_234, LevelCalculator.stateFor(1_234).totalXp)
        assertEquals(Int.MAX_VALUE, LevelCalculator.stateFor(Int.MAX_VALUE).totalXp)
    }
}
