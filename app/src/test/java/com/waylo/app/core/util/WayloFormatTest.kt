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
    fun distanceShowsMetersBelowOneKilometer() {
        assertEquals("0 m", WayloFormat.distance(0.0))
        assertEquals("245 m", WayloFormat.distance(0.245))
        assertEquals("999 m", WayloFormat.distance(0.999))
    }

    @Test
    fun distanceShowsKilometersFromOneKilometerOnwards() {
        assertEquals("1.24 km", WayloFormat.distance(1.24))
        assertEquals("10.00 km", WayloFormat.distance(10.0))
        assertEquals("5.00 km", WayloFormat.distance(5.0))
        assertEquals("5.44 km", WayloFormat.distance(5.44))
    }

    @Test
    fun distanceRoundsMetersBoundaryToKilometers() {
        assertEquals("1.00 km", WayloFormat.distance(0.9996))
    }

    @Test
    fun distanceClampsInvalidValuesToZero() {
        assertEquals("0 km", WayloFormat.distance(-1.0))
        assertEquals("0 km", WayloFormat.distance(Double.NaN))
    }

    @Test
    fun paceFormatsMinutesAndSecondsPerKilometer() {
        assertEquals("6:30 /km", WayloFormat.pace(390.0))
        assertEquals("7:00 /km", WayloFormat.pace(420.0))
        assertEquals("1:05 /km", WayloFormat.pace(65.0))
        assertEquals("1:00 /km", WayloFormat.pace(60.0))
    }

    @Test
    fun paceIsUnavailableForInvalidValues() {
        assertEquals(WayloFormat.DASH, WayloFormat.pace(null))
        assertEquals(WayloFormat.DASH, WayloFormat.pace(0.0))
        assertEquals(WayloFormat.DASH, WayloFormat.pace(-30.0))
        assertEquals(WayloFormat.DASH, WayloFormat.pace(Double.NaN))
        assertEquals(WayloFormat.DASH, WayloFormat.pace(Double.POSITIVE_INFINITY))
    }

    @Test
    fun speedFormatsKilometersPerHour() {
        assertEquals("7.2 km/h", WayloFormat.speed(2.0))
        val expected = String.format(Locale.getDefault(), "%.1f", 1.333 * 3.6) + " km/h"
        assertEquals(expected, WayloFormat.speed(1.333))
    }

    @Test
    fun speedIsUnavailableForInvalidValues() {
        assertEquals(WayloFormat.DASH, WayloFormat.speed(null))
        assertEquals(WayloFormat.DASH, WayloFormat.speed(-1.0))
        assertEquals(WayloFormat.DASH, WayloFormat.speed(Double.NaN))
    }

    @Test
    fun caloriesFormatAsWholeNumbers() {
        assertEquals("0", WayloFormat.calories(0.0))
        assertEquals("144", WayloFormat.calories(143.6))
        assertEquals(WayloFormat.DASH, WayloFormat.calories(null))
        assertEquals(WayloFormat.DASH, WayloFormat.calories(-5.0))
    }

    @Test
    fun stepsFormatWithGroupingOrDash() {
        assertEquals(NumberFormat.getIntegerInstance().format(2_860L), WayloFormat.steps(2_860L))
        assertEquals("0", WayloFormat.steps(0L))
        assertEquals(WayloFormat.DASH, WayloFormat.steps(null))
        assertEquals(WayloFormat.DASH, WayloFormat.steps(-1L))
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
