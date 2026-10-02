package com.waylo.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class WayloFormatTest {

    @Test
    fun countFormatsZero() {
        assertEquals("0", WayloFormat.count(0))
    }

    @Test
    fun countUsesLocaleGrouping() {
        val expected = NumberFormat.getIntegerInstance().format(6_000L)
        assertEquals(expected, WayloFormat.count(6_000))
    }

    @Test
    fun countFormatsLongValues() {
        assertEquals(NumberFormat.getIntegerInstance().format(3_482L), WayloFormat.count(3_482L))
        assertEquals(NumberFormat.getIntegerInstance().format(1_000_000L), WayloFormat.count(1_000_000L))
    }

    @Test
    fun countAgreesAcrossIntAndLong() {
        assertEquals(WayloFormat.count(6_000), WayloFormat.count(6_000L))
        assertEquals(WayloFormat.count(0), WayloFormat.count(0L))
    }

    @Test
    fun distanceFormatsWholeKilometers() {
        assertEquals("0 km", WayloFormat.distance(0.0))
        assertEquals("5 km", WayloFormat.distance(5.0))
    }

    @Test
    fun distanceRoundsFractionalValues() {
        val expected = String.format(Locale.getDefault(), "%.1f", 5.4) + " km"

        assertEquals(expected, WayloFormat.distance(5.44))
    }

    @Test
    fun minutesIncludeUnit() {
        assertEquals("0 min", WayloFormat.minutes(0))
        assertEquals("${WayloFormat.count(42)} min", WayloFormat.minutes(42))
    }

    @Test
    fun durationFormatsZeroAsBlankSlate() {
        assertEquals("00:00:00", WayloFormat.duration(0))
    }

    @Test
    fun durationPadsHoursMinutesAndSeconds() {
        assertEquals("00:00:01", WayloFormat.duration(1_000))
        assertEquals("00:23:41", WayloFormat.duration(1_421_000))
        assertEquals("01:00:00", WayloFormat.duration(3_600_000))
        assertEquals("02:01:05", WayloFormat.duration(7_265_000))
    }

    @Test
    fun durationIgnoresPartialSeconds() {
        assertEquals("00:00:01", WayloFormat.duration(1_999))
    }

    @Test
    fun durationClampsNegativeValuesToZero() {
        assertEquals("00:00:00", WayloFormat.duration(-5))
    }
}
