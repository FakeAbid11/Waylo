package com.waylo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkRouteTest {

    @Test
    fun emptyRouteHasNoSegmentsBoundsOrLatestPoint() {
        val route = WalkRoute()

        assertTrue(route.isEmpty)
        assertNull(route.latestOrNull())
        assertTrue(route.segments().isEmpty())
        assertNull(route.boundsOrNull())
    }

    @Test
    fun pointsAppendInArrivalOrder() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0))
            .withPoint(point(sequence = 1, latitude = 52.001))
            .withPoint(point(sequence = 2, latitude = 52.002))

        assertEquals(listOf(0, 1, 2), route.points.map { it.sequence })
        assertEquals(52.002, route.latestOrNull()!!.latitude, 0.0)
        assertFalse(route.isEmpty)
    }

    @Test
    fun markingABreakOnAnEmptyRouteDoesNothing() {
        val route = WalkRoute().markBreakBeforeNextPoint()

        assertEquals(WalkRoute(), route)
    }

    @Test
    fun routeWithoutABreakIsOneSegment() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0))
            .withPoint(point(sequence = 1, latitude = 52.001))

        val segments = route.segments()
        assertEquals(1, segments.size)
        assertEquals(listOf(0, 1), segments.first().map { it.sequence })
    }

    @Test
    fun pauseBreaksTheRouteIntoTwoSegmentsAfterTheNextPoint() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0))
            .withPoint(point(sequence = 1, latitude = 52.001))
            .markBreakBeforeNextPoint()
            .withPoint(point(sequence = 2, latitude = 52.002))

        val segments = route.segments()
        assertEquals(2, segments.size)
        assertEquals(listOf(0, 1), segments[0].map { it.sequence })
        assertEquals(listOf(2), segments[1].map { it.sequence })
    }

    @Test
    fun repeatedPausesWithoutNewPointsKeepASingleBreak() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0))
            .markBreakBeforeNextPoint()
            .markBreakBeforeNextPoint()

        assertEquals(setOf(1), route.breakBeforeIndexes)
        assertEquals(1, route.segments().size)
    }

    @Test
    fun boundsCoverEveryPoint() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))
            .withPoint(point(sequence = 1, latitude = 52.01, longitude = 12.99))
            .withPoint(point(sequence = 2, latitude = 51.99, longitude = 13.02))

        val bounds = route.boundsOrNull()!!
        assertEquals(51.99, bounds.minLatitude, 0.0)
        assertEquals(52.01, bounds.maxLatitude, 0.0)
        assertEquals(12.99, bounds.minLongitude, 0.0)
        assertEquals(13.02, bounds.maxLongitude, 0.0)
        assertEquals(52.0, bounds.centerLatitude, 0.0)
        assertEquals(13.005, bounds.centerLongitude, 0.0001)
        assertTrue(bounds.hasArea)
    }

    @Test
    fun singlePointBoundsHaveNoArea() {
        val route = WalkRoute().withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))

        val bounds = route.boundsOrNull()!!
        assertEquals(52.0, bounds.centerLatitude, 0.0)
        assertEquals(13.0, bounds.centerLongitude, 0.0)
        assertFalse(bounds.hasArea)
    }

    private fun point(
        sequence: Int,
        latitude: Double,
        longitude: Double = 13.0,
    ): WalkingLocationPoint = WalkingLocationPoint(
        sessionId = 1L,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = 1_000L + sequence,
    )
}
