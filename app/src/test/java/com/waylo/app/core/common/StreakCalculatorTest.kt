package com.waylo.app.core.common

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 3)

    @Test
    fun noQualifiedDaysMeansNoStreak() {
        val result = StreakCalculator.calculate(emptySet(), today)

        assertEquals(0, result.currentStreakDays)
        assertEquals(0, result.longestStreakDays)
    }

    @Test
    fun aQualifyingDayTodayStartsTheStreak() {
        val result = StreakCalculator.calculate(setOf(today), today)

        assertEquals(1, result.currentStreakDays)
        assertEquals(1, result.longestStreakDays)
    }

    @Test
    fun yesterdayQualifyingKeepsTheCurrentStreakAlive() {
        val result = StreakCalculator.calculate(
            setOf(today.minusDays(1), today.minusDays(2)),
            today,
        )

        assertEquals(2, result.currentStreakDays)
        assertEquals(2, result.longestStreakDays)
    }

    @Test
    fun missingBothTodayAndYesterdayEndsTheCurrentStreak() {
        val result = StreakCalculator.calculate(
            setOf(today.minusDays(2), today.minusDays(3)),
            today,
        )

        assertEquals(0, result.currentStreakDays)
        assertEquals(2, result.longestStreakDays)
    }

    @Test
    fun aGapResetsTheCurrentStreakButKeepsTheLongestRecord() {
        val result = StreakCalculator.calculate(
            setOf(
                today.minusDays(4),
                today.minusDays(3),
                today.minusDays(2),
                today,
            ),
            today,
        )

        assertEquals(1, result.currentStreakDays)
        assertEquals(3, result.longestStreakDays)
    }

    @Test
    fun multipleAwardsOnOneDayCountAsOneDay() {
        val result = StreakCalculator.calculate(
            setOf(today, today, today.minusDays(1)),
            today,
        )

        assertEquals(2, result.currentStreakDays)
        assertEquals(2, result.longestStreakDays)
    }

    @Test
    fun unsortedInputIsStillMeasuredAsConsecutiveDays() {
        val result = StreakCalculator.calculate(
            setOf(today.minusDays(2), today, today.minusDays(1)),
            today,
        )

        assertEquals(3, result.currentStreakDays)
        assertEquals(3, result.longestStreakDays)
    }

    @Test
    fun theLongestStreakSurvivesEvenWhenTheCurrentOneEnds() {
        val result = StreakCalculator.calculate(
            setOf(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 4),
                today.minusDays(1),
            ),
            today,
        )

        assertEquals(1, result.currentStreakDays)
        assertEquals(4, result.longestStreakDays)
    }

    @Test
    fun aFutureDatedAwardDoesNotCreateACurrentStreak() {
        val result = StreakCalculator.calculate(
            setOf(today.plusDays(5)),
            today,
        )

        assertEquals(0, result.currentStreakDays)
        assertEquals(1, result.longestStreakDays)
    }
}
