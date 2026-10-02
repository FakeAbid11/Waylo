package com.waylo.app.data.location

import com.waylo.app.domain.model.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameworkDistanceTest {

    private fun sample(latitude: Double, longitude: Double = 13.0) =
        LocationSample(latitude, longitude, 1_700_000_000_000L, 5f)

    @Test
    fun latitudeOffsetMatchesTheKnownDegreeLength() {
        val distance = FrameworkDistance.between(sample(52.0), sample(52.001))

        assertEquals(111.19, distance, 1.0)
    }

    @Test
    fun identicalPointsAreZeroMetersApart() {
        val distance = FrameworkDistance.between(sample(52.0), sample(52.0))

        assertEquals(0.0, distance, 0.0)
    }

    @Test
    fun distanceIsSymmetric() {
        val forward = FrameworkDistance.between(sample(52.0), sample(52.01))
        val backward = FrameworkDistance.between(sample(52.01), sample(52.0))

        assertEquals(forward, backward, 0.01)
    }
}
