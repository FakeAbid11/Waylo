package com.waylo.app.data.map

import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapRoutePresentationTest {

    @Test
    fun emptyRouteProducesNoGeoJson() {
        assertNull(MapRoutePresentation.routeGeoJson(WalkRoute()))
        assertNull(MapRoutePresentation.markerGeoJson(WalkRoute()))
    }

    @Test
    fun singleSegmentRouteIsOneLineStringGeometry() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))
            .withPoint(point(sequence = 1, latitude = 52.001, longitude = 13.001))

        assertEquals(
            """{"type":"Feature","geometry":{"type":"MultiLineString","coordinates":[[[13.0,52.0],[13.001,52.001]]]}}""",
            MapRoutePresentation.routeGeoJson(route),
        )
    }

    @Test
    fun pausedRouteKeepsTwoLineSegmentsWithoutAFakeConnection() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))
            .withPoint(point(sequence = 1, latitude = 52.001, longitude = 13.001))
            .markBreakBeforeNextPoint()
            .withPoint(point(sequence = 2, latitude = 52.002, longitude = 13.002))

        assertEquals(
            """{"type":"Feature","geometry":{"type":"MultiLineString","coordinates":[[[13.0,52.0],[13.001,52.001]],[[13.002,52.002]]]}}""",
            MapRoutePresentation.routeGeoJson(route),
        )
    }

    @Test
    fun markerFollowsTheLatestRecordedPoint() {
        val route = WalkRoute()
            .withPoint(point(sequence = 0, latitude = 52.0, longitude = 13.0))
            .withPoint(point(sequence = 1, latitude = 52.001, longitude = 13.001))

        assertEquals(
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[13.001,52.001]}}""",
            MapRoutePresentation.markerGeoJson(route),
        )
    }

    @Test
    fun emptyCollectionMasksAnyExistingGeometry() {
        assertEquals(
            """{"type":"FeatureCollection","features":[]}""",
            MapRoutePresentation.EMPTY_FEATURE_COLLECTION,
        )
    }

    private fun point(
        sequence: Int,
        latitude: Double,
        longitude: Double,
    ): WalkingLocationPoint = WalkingLocationPoint(
        sessionId = 1L,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = 1_000L + sequence,
    )
}
