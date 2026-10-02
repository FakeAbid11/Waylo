package com.waylo.app.core.common

import com.waylo.app.domain.model.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot

class WalkingEngineTest {

    private fun sample(
        latitude: Double = 52.0,
        longitude: Double = 13.0,
        timestampMillis: Long = 1_700_000_000_000L,
        accuracyMeters: Float? = 5f,
    ) = LocationSample(latitude, longitude, timestampMillis, accuracyMeters)

    private fun measure(from: LocationSample, to: LocationSample): Double {
        val meanLatitude = Math.toRadians((from.latitude + to.latitude) / 2.0)
        val east = Math.toRadians(to.longitude - from.longitude) * cos(meanLatitude) * EARTH_RADIUS_METERS
        val north = Math.toRadians(to.latitude - from.latitude) * EARTH_RADIUS_METERS
        return hypot(east, north)
    }

    private fun engine(
        initialDistanceMeters: Double = 0.0,
        maxAccuracyMeters: Float = WalkingEngine.MAX_ACCURACY_METERS,
        maxSpeedMps: Double = WalkingEngine.MAX_SPEED_MPS,
        measureDistance: (LocationSample, LocationSample) -> Double = ::measure,
    ) = WalkingEngine(
        measureDistance = measureDistance,
        initialDistanceMeters = initialDistanceMeters,
        maxAccuracyMeters = maxAccuracyMeters,
        maxSpeedMps = maxSpeedMps,
    )

    @Test
    fun firstAcceptedSampleAnchorsWithZeroDistance() {
        val engine = engine()

        assertTrue(engine.submit(sample()))

        assertEquals(0.0, engine.distanceMeters, 0.0)
        assertTrue(engine.hasAnchor)
    }

    @Test
    fun walkingNorthAccumulatesTheLatitudeOffset() {
        val engine = engine()

        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))
        engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L))

        assertEquals(111.19, engine.distanceMeters, 0.05)
    }

    @Test
    fun cumulativeDistanceAddsEachAcceptedSegment() {
        val engine = engine()

        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))
        engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L))
        engine.submit(sample(latitude = 52.002, timestampMillis = 1_060_000L))

        assertEquals(222.38, engine.distanceMeters, 0.1)
    }

    @Test
    fun stationarySamplesAreAcceptedButAddNoDistance() {
        val engine = engine()

        engine.submit(sample(timestampMillis = 1_000_000L))
        val accepted = engine.submit(sample(timestampMillis = 1_030_000L))

        assertTrue(accepted)
        assertEquals(0.0, engine.distanceMeters, 0.0)
    }

    @Test
    fun invalidCoordinatesAreRejectedBeforeAnchoring() {
        val engine = engine()

        assertFalse(engine.submit(sample(latitude = Double.NaN)))
        assertFalse(engine.submit(sample(longitude = Double.POSITIVE_INFINITY)))
        assertFalse(engine.submit(sample(latitude = 91.0)))
        assertFalse(engine.submit(sample(longitude = -181.0)))
        assertFalse(engine.submit(sample(timestampMillis = 0L)))

        assertEquals(0.0, engine.distanceMeters, 0.0)
        assertFalse(engine.hasAnchor)
    }

    @Test
    fun accuracyWorseThanTheThresholdIsRejected() {
        val engine = engine()

        assertFalse(engine.submit(sample(accuracyMeters = 51f)))

        assertFalse(engine.hasAnchor)
    }

    @Test
    fun unknownAccuracyIsAccepted() {
        val engine = engine()

        assertTrue(engine.submit(sample(accuracyMeters = null)))
    }

    @Test
    fun nonFiniteOrNegativeAccuracyIsRejected() {
        val engine = engine()

        assertFalse(engine.submit(sample(accuracyMeters = Float.NaN)))
        assertFalse(engine.submit(sample(accuracyMeters = Float.POSITIVE_INFINITY)))
        assertFalse(engine.submit(sample(accuracyMeters = -1f)))
    }

    @Test
    fun duplicateAndBackwardsTimestampsAreRejected() {
        val engine = engine()
        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))

        assertFalse(engine.submit(sample(latitude = 52.001, timestampMillis = 1_000_000L)))
        assertFalse(engine.submit(sample(latitude = 52.001, timestampMillis = 900_000L)))

        assertEquals(0.0, engine.distanceMeters, 0.0)
    }

    @Test
    fun impossibleSpeedJumpIsRejectedAndDoesNotMoveTheAnchor() {
        val engine = engine()
        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))

        val jumped = engine.submit(sample(latitude = 52.01, timestampMillis = 1_001_000L))
        assertFalse(jumped)
        assertEquals(0.0, engine.distanceMeters, 0.0)

        val next = engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L))
        assertTrue(next)
        assertEquals(111.19, engine.distanceMeters, 0.05)
    }

    @Test
    fun poorAccuracySampleNeverBecomesTheAnchor() {
        val engine = engine()
        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))

        assertFalse(engine.submit(sample(latitude = 52.002, timestampMillis = 1_030_000L, accuracyMeters = 200f)))

        assertTrue(engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L)))
        assertEquals(111.19, engine.distanceMeters, 0.05)
    }

    @Test
    fun nonFiniteMeasuredDistanceIsRejected() {
        val engine = engine(measureDistance = { _, _ -> Double.NaN })
        engine.submit(sample(timestampMillis = 1_000_000L))

        assertFalse(engine.submit(sample(timestampMillis = 1_030_000L)))
        assertEquals(0.0, engine.distanceMeters, 0.0)
    }

    @Test
    fun resetAnchorMakesTheNextSampleAFreshStart() {
        val engine = engine()
        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))
        engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L))
        assertEquals(111.19, engine.distanceMeters, 0.05)

        engine.resetAnchor()

        assertTrue(engine.submit(sample(latitude = 53.5, timestampMillis = 2_000_000L)))
        assertEquals(111.19, engine.distanceMeters, 0.05)
    }

    @Test
    fun negativeInitialDistanceIsClampedToZero() {
        val engine = engine(initialDistanceMeters = -10.0)

        assertEquals(0.0, engine.distanceMeters, 0.0)
    }

    @Test
    fun restoredInitialDistanceIsKept() {
        val engine = engine(initialDistanceMeters = 1_024.5)

        assertEquals(1_024.5, engine.distanceMeters, 0.0)
    }

    @Test
    fun stricterSpeedThresholdRejectsBriskMovement() {
        val engine = engine(maxSpeedMps = 0.5)
        engine.submit(sample(latitude = 52.0, timestampMillis = 1_000_000L))

        assertFalse(engine.submit(sample(latitude = 52.001, timestampMillis = 1_030_000L)))

        assertEquals(0.0, engine.distanceMeters, 0.0)
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
