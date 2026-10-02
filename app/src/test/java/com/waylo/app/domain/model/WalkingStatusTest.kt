package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingStatusTest {

    @Test
    fun idleStatusHasNoElapsedWalkingTime() {
        val status = WalkingStatus(state = WalkingState.Idle)

        assertEquals(0L, status.activeMillisAt(1_000_000L))
    }

    @Test
    fun activeStatusAddsTheRunningSegment() {
        val status = WalkingStatus(
            state = WalkingState.Active,
            activeMillis = 60_000L,
            activeSegmentStartMillis = 1_000_000L,
        )

        assertEquals(90_000L, status.activeMillisAt(1_030_000L))
    }

    @Test
    fun pausedStatusIgnoresTheSegmentStart() {
        val status = WalkingStatus(
            state = WalkingState.Paused,
            activeMillis = 60_000L,
            activeSegmentStartMillis = 1_000_000L,
        )

        assertEquals(60_000L, status.activeMillisAt(1_030_000L))
    }

    @Test
    fun elapsedTimeNeverGoesNegative() {
        val status = WalkingStatus(
            state = WalkingState.Active,
            activeMillis = 60_000L,
            activeSegmentStartMillis = 1_000_000L,
        )

        assertEquals(60_000L, status.activeMillisAt(900_000L))
    }

    @Test
    fun startingStatusOnlyReportsFoldedTime() {
        val status = WalkingStatus(
            state = WalkingState.Starting,
            activeMillis = 5_000L,
            activeSegmentStartMillis = 1_000_000L,
        )

        assertEquals(5_000L, status.activeMillisAt(1_030_000L))
    }
}
