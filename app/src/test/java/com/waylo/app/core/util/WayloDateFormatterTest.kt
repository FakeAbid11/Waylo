package com.waylo.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WayloDateFormatterTest {

    private val utc: ZoneId = ZoneId.of("UTC")
    private val newYork: ZoneId = ZoneId.of("America/New_York")

    @Test
    fun sameLocalDayIsLabelledToday() {
        val now = epoch(LocalDateTime.of(2026, 10, 3, 12, 0), utc)

        val day = WayloDateFormatter.localDate(now, utc)

        assertEquals(LocalDate.of(2026, 10, 3), day)
        assertEquals("Today", WayloDateFormatter.dayLabel(day, day))
    }

    @Test
    fun thePreviousLocalDayIsLabelledYesterday() {
        val now = epoch(LocalDateTime.of(2026, 10, 3, 0, 30), utc)
        val walk = epoch(LocalDateTime.of(2026, 10, 2, 23, 30), utc)

        val label = WayloDateFormatter.dayLabel(
            WayloDateFormatter.localDate(walk, utc),
            WayloDateFormatter.localDate(now, utc),
        )

        assertEquals("Yesterday", label)
    }

    @Test
    fun olderDaysFallBackToTheAbbreviatedDate() {
        val now = epoch(LocalDateTime.of(2026, 10, 3, 12, 0), utc)
        val walk = epoch(LocalDateTime.of(2026, 9, 28, 6, 42), utc)

        val label = WayloDateFormatter.dayLabel(
            WayloDateFormatter.localDate(walk, utc),
            WayloDateFormatter.localDate(now, utc),
        )

        assertEquals("Sep 28, 2026", label)
    }

    @Test
    fun theLocalDateIsDecidedInTheUsersZoneNotUtc() {
        val instant = epoch(LocalDateTime.of(2026, 10, 3, 1, 0), utc)

        val utcDay = WayloDateFormatter.localDate(instant, utc)
        val newYorkDay = WayloDateFormatter.localDate(instant, newYork)

        assertEquals(LocalDate.of(2026, 10, 3), utcDay)
        assertEquals(LocalDate.of(2026, 10, 2), newYorkDay)
    }

    @Test
    fun timeOfDayConvertsToTheGivenZone() {
        val instant = epoch(LocalDateTime.of(2026, 10, 3, 18, 42), utc)

        assertEquals("6:42 PM", WayloDateFormatter.timeOfDay(instant, utc))
        assertEquals("2:42 PM", WayloDateFormatter.timeOfDay(instant, newYork))
    }

    @Test
    fun midnightRendersAsTwelveAM() {
        val instant = epoch(LocalDateTime.of(2026, 10, 3, 0, 0), utc)

        assertEquals("12:00 AM", WayloDateFormatter.timeOfDay(instant, utc))
    }

    @Test
    fun aYearBoundaryKeepsEachMomentInItsOwnLocalDay() {
        val newYearsEve = epoch(LocalDateTime.of(2026, 12, 31, 23, 55), utc)
        val newYearsDay = epoch(LocalDateTime.of(2027, 1, 1, 0, 5), utc)

        assertEquals(LocalDate.of(2026, 12, 31), WayloDateFormatter.localDate(newYearsEve, utc))
        assertEquals(LocalDate.of(2027, 1, 1), WayloDateFormatter.localDate(newYearsDay, utc))
        assertEquals(
            "Jan 1, 2027",
            WayloDateFormatter.dayLabel(
                WayloDateFormatter.localDate(newYearsDay, utc),
                WayloDateFormatter.localDate(newYearsEve, utc),
            ),
        )
    }

    @Test
    fun fullDateRendersMonthDayAndYear() {
        val day = LocalDate.of(2026, 9, 28)

        assertEquals("September 28, 2026", WayloDateFormatter.fullDate(day))
    }

    @Test
    fun historyTimestampCombinesTheRelativeLabelWithTheTime() {
        val now = epoch(LocalDateTime.of(2026, 10, 3, 21, 0), utc)
        val walk = epoch(LocalDateTime.of(2026, 10, 3, 18, 42), utc)

        assertEquals(
            "Today · 6:42 PM",
            WayloDateFormatter.historyTimestamp(walk, now, utc),
        )
    }

    private fun epoch(localDateTime: LocalDateTime, zone: ZoneId): Long =
        localDateTime.atZone(zone).toInstant().toEpochMilli()
}
