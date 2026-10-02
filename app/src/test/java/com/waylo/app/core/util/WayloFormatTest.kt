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
}
