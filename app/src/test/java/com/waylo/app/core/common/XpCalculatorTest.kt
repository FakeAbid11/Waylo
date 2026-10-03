package com.waylo.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class XpCalculatorTest {

    @Test
    fun belowTheMinimumQualifyingDistanceEarnsNothing() {
        assertEquals(0, XpCalculator.xpFor(0.0))
        assertEquals(0, XpCalculator.xpFor(99.9))
        assertEquals(0, XpCalculator.xpFor(-1_500.0))
    }

    @Test
    fun exactlyTheMinimumQualifyingDistanceQualifies() {
        assertEquals(10, XpCalculator.xpFor(XpCalculator.MIN_QUALIFYING_DISTANCE_METERS))
        assertEquals(10, XpCalculator.xpFor(100.4))
    }

    @Test
    fun xpIsRoundedFromKilometersTimesOneHundred() {
        assertEquals(150, XpCalculator.xpFor(1_500.0))
        assertEquals(150, XpCalculator.xpFor(1_504.0))
        assertEquals(151, XpCalculator.xpFor(1_505.0))
        assertEquals(153, XpCalculator.xpFor(1_532.0))
        assertEquals(2_100, XpCalculator.xpFor(21_000.0))
    }

    @Test
    fun nonFiniteDistancesEarnNothing() {
        assertEquals(0, XpCalculator.xpFor(Double.NaN))
        assertEquals(0, XpCalculator.xpFor(Double.POSITIVE_INFINITY))
        assertEquals(0, XpCalculator.xpFor(Double.NEGATIVE_INFINITY))
    }
}
