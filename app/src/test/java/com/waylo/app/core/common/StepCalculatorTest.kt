package com.waylo.app.core.common

import com.waylo.app.domain.model.StepRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class StepCalculatorTest {

    @Test
    fun firstReadingAnchorsBaselineAndCountsZero() {
        val record = applySensorValue(
            record = StepRecord(epochDay = 100L),
            rawSensorValue = 5_000L,
            todayEpochDay = 100L,
        )

        assertEquals(100L, record.epochDay)
        assertEquals(5_000L, record.baselineSensorCount)
        assertEquals(5_000L, record.lastSensorCount)
        assertEquals(0L, record.todaySteps)
    }

    @Test
    fun readingAdvancesTodayStepsFromBaseline() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 5_000L,
            todaySteps = 0L,
        )

        val updated = applySensorValue(record, rawSensorValue = 7_500L, todayEpochDay = 100L)

        assertEquals(2_500L, updated.todaySteps)
        assertEquals(7_500L, updated.lastSensorCount)
        assertEquals(5_000L, updated.baselineSensorCount)
    }

    @Test
    fun repeatedSameReadingKeepsStepsStable() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        val updated = applySensorValue(record, rawSensorValue = 7_500L, todayEpochDay = 100L)

        assertEquals(2_500L, updated.todaySteps)
        assertEquals(7_500L, updated.lastSensorCount)
    }

    @Test
    fun staleOutOfOrderReadingIsIgnored() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 6_000L,
            todaySteps = 1_000L,
        )

        val updated = applySensorValue(record, rawSensorValue = 5_500L, todayEpochDay = 100L)

        assertEquals(record, updated)
    }

    @Test
    fun counterResetBelowBaselineRebasesSafely() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 50_000L,
            lastSensorCount = 50_500L,
            todaySteps = 500L,
        )

        val updated = applySensorValue(record, rawSensorValue = 100L, todayEpochDay = 100L)

        assertEquals(100L, updated.baselineSensorCount)
        assertEquals(100L, updated.lastSensorCount)
        assertEquals(0L, updated.todaySteps)
    }

    @Test
    fun rolloverStartsNewDayWithoutCarryingSteps() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        val rolled = rollToDay(record, todayEpochDay = 101L)

        assertEquals(101L, rolled.epochDay)
        assertEquals(0L, rolled.todaySteps)
        assertEquals(7_500L, rolled.baselineSensorCount)
        assertEquals(7_500L, rolled.lastSensorCount)
    }

    @Test
    fun readingAfterRolloverCountsFromNewBaseline() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        val updated = applySensorValue(record, rawSensorValue = 7_600L, todayEpochDay = 101L)

        assertEquals(101L, updated.epochDay)
        assertEquals(100L, updated.todaySteps)
        assertEquals(7_600L, updated.lastSensorCount)
    }

    @Test
    fun multiDayGapStartsWithoutAnchoringYesterday() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        val rolled = rollToDay(record, todayEpochDay = 105L)

        assertEquals(105L, rolled.epochDay)
        assertEquals(0L, rolled.todaySteps)
        assertEquals(null, rolled.baselineSensorCount)
    }

    @Test
    fun firstEverRecordOnlyRecordsTheDay() {
        val rolled = rollToDay(StepRecord(), todayEpochDay = 200L)

        assertEquals(200L, rolled.epochDay)
        assertEquals(null, rolled.baselineSensorCount)
        assertEquals(null, rolled.lastSensorCount)
        assertEquals(0L, rolled.todaySteps)
    }

    @Test
    fun sameDayRollIsANoOp() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        assertEquals(record, rollToDay(record, todayEpochDay = 100L))
    }

    @Test
    fun clockMovedBackwardsKeepsCurrentRecord() {
        val record = StepRecord(
            epochDay = 105L,
            baselineSensorCount = 5_000L,
            lastSensorCount = 7_500L,
            todaySteps = 2_500L,
        )

        assertEquals(record, rollToDay(record, todayEpochDay = 103L))
    }

    @Test
    fun sanitizeDropsNegativeSteps() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 10L,
            lastSensorCount = 20L,
            todaySteps = -50L,
        )

        val sanitized = sanitizeRecord(record)

        assertEquals(0L, sanitized.todaySteps)
        assertEquals(10L, sanitized.baselineSensorCount)
        assertEquals(20L, sanitized.lastSensorCount)
    }

    @Test
    fun sanitizeWithoutBaselineDropsSteps() {
        val record = StepRecord(epochDay = 100L, todaySteps = 100L)

        val sanitized = sanitizeRecord(record)

        assertEquals(null, sanitized.baselineSensorCount)
        assertEquals(null, sanitized.lastSensorCount)
        assertEquals(0L, sanitized.todaySteps)
    }

    @Test
    fun sanitizeFillsMissingLastSensorFromBaseline() {
        val record = StepRecord(
            epochDay = 100L,
            baselineSensorCount = 42L,
            lastSensorCount = null,
            todaySteps = 0L,
        )

        val sanitized = sanitizeRecord(record)

        assertEquals(42L, sanitized.baselineSensorCount)
        assertEquals(42L, sanitized.lastSensorCount)
    }
}
