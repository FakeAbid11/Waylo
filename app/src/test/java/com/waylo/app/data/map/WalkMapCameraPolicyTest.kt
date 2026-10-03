package com.waylo.app.data.map

import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkMapCameraPolicyTest {

    @Test
    fun initialStateWaitsForTheStyle() {
        assertEquals(WalkMapLoadState.Loading, WalkMapState().loadState)
    }

    @Test
    fun loadedStyleWithoutNetworkIsUnavailable() {
        val state = WalkMapCameraPolicy.onStyleLoaded(WalkMapState())
        assertEquals(WalkMapLoadState.Ready, state.loadState)

        val offline = WalkMapCameraPolicy.onNetworkChanged(state, available = false)
        assertEquals(WalkMapLoadState.Unavailable, offline.loadState)
    }

    @Test
    fun offlineWinsOverAStyleFailureMessage() {
        val failed = WalkMapCameraPolicy.onStyleFailed(WalkMapState())
        assertEquals(WalkMapLoadState.StyleError, failed.loadState)

        val offline = WalkMapCameraPolicy.onNetworkChanged(failed, available = false)
        assertEquals(WalkMapLoadState.Unavailable, offline.loadState)
    }

    @Test
    fun retryClearsTheStyleFailureAndBumpsTheGeneration() {
        val failed = WalkMapCameraPolicy.onStyleFailed(WalkMapState())

        val retried = WalkMapCameraPolicy.onRetryStyle(failed)

        assertEquals(WalkMapLoadState.Loading, retried.loadState)
        assertEquals(1L, retried.styleGeneration)
        assertFalse(retried.styleFailed)
    }

    @Test
    fun comingBackOnlineWithAnUnloadedStyleRetriesAutomatically() {
        val offline = WalkMapCameraPolicy.onNetworkChanged(WalkMapState(), available = false)

        val online = WalkMapCameraPolicy.onNetworkChanged(offline, available = true)

        assertEquals(WalkMapLoadState.Loading, online.loadState)
        assertEquals(1L, online.styleGeneration)
    }

    @Test
    fun comingBackOnlineWithALoadedStyleDoesNotReloadIt() {
        val loaded = WalkMapCameraPolicy.onStyleLoaded(WalkMapState())
        val offline = WalkMapCameraPolicy.onNetworkChanged(loaded, available = false)

        val online = WalkMapCameraPolicy.onNetworkChanged(offline, available = true)

        assertEquals(WalkMapLoadState.Ready, online.loadState)
        assertEquals(0L, online.styleGeneration)
    }

    @Test
    fun firstFixCentersTheCameraWhileFollowing() {
        val state = WalkMapCameraPolicy.onFix(WalkMapState(), MapLatLng(52.0, 13.0))

        assertTrue(state.followEnabled)
        assertEquals(MapLatLng(52.0, 13.0), state.lastFix)
        assertEquals(MapLatLng(52.0, 13.0), state.appliedFix)
        val command = state.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(MapLatLng(52.0, 13.0), command.target)
        assertEquals(WalkMapCameraPolicy.WALKING_ZOOM, command.zoom, 0.0)
        assertEquals(1L, command.id)
    }

    @Test
    fun tinyMovementsDoNotChurnTheCamera() {
        val first = WalkMapCameraPolicy.onFix(WalkMapState(), MapLatLng(52.0, 13.0))
        val commandBefore = first.cameraCommand

        val nudged = WalkMapCameraPolicy.onFix(first, MapLatLng(52.00001, 13.0))

        assertSame(commandBefore, nudged.cameraCommand)
        assertEquals(MapLatLng(52.00001, 13.0), nudged.lastFix)
    }

    @Test
    fun meaningfulMovementsIssueANewFollowCommand() {
        val first = WalkMapCameraPolicy.onFix(WalkMapState(), MapLatLng(52.0, 13.0))

        val moved = WalkMapCameraPolicy.onFix(first, MapLatLng(52.001, 13.0))

        val command = moved.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(2L, command.id)
        assertEquals(MapLatLng(52.001, 13.0), command.target)
    }

    @Test
    fun manualPanningStopsFollowingUntilRecentered() {
        val withFix = WalkMapCameraPolicy.onFix(WalkMapState(), MapLatLng(52.0, 13.0))

        val panned = WalkMapCameraPolicy.onMapPan(withFix)
        assertFalse(panned.followEnabled)

        val moved = WalkMapCameraPolicy.onFix(panned, MapLatLng(52.01, 13.0))
        assertSame(withFix.cameraCommand, moved.cameraCommand)
        assertEquals(MapLatLng(52.01, 13.0), moved.lastFix)

        val recentered = WalkMapCameraPolicy.onRecenter(moved)
        assertTrue(recentered.followEnabled)
        val command = recentered.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(MapLatLng(52.01, 13.0), command.target)
        assertEquals(WalkMapCameraPolicy.WALKING_ZOOM, command.zoom, 0.0)
        assertTrue(command.id > (withFix.cameraCommand!!.id))
    }

    @Test
    fun recenterWithoutAFixChangesNothing() {
        val state = WalkMapState(followEnabled = false)

        val recentered = WalkMapCameraPolicy.onRecenter(state)

        assertNull(recentered.cameraCommand)
        assertFalse(recentered.followEnabled)
        assertNull(recentered.lastFix)
    }

    @Test
    fun completingAFullRouteFitsTheBounds() {
        val route = routeWithPoints(
            MapLatLng(52.0, 13.0),
            MapLatLng(52.01, 13.02),
        )

        val completed = WalkMapCameraPolicy.onWalkCompleted(WalkMapState(), route)

        assertFalse(completed.followEnabled)
        assertTrue(completed.completionCameraApplied)
        val command = completed.cameraCommand as MapCameraCommand.FitBounds
        assertEquals(52.0, command.bounds.minLatitude, 0.0)
        assertEquals(52.01, command.bounds.maxLatitude, 0.0)
        assertEquals(13.0, command.bounds.minLongitude, 0.0)
        assertEquals(13.02, command.bounds.maxLongitude, 0.0)
    }

    @Test
    fun completingAPausedRouteStillFitsEverySegmentPoint() {
        val route = WalkRoute()
            .withPoint(point(0, MapLatLng(52.0, 13.0)))
            .withPoint(point(1, MapLatLng(52.02, 13.03)))
            .markBreakBeforeNextPoint()
            .withPoint(point(2, MapLatLng(51.99, 13.01)))

        val completed = WalkMapCameraPolicy.onWalkCompleted(WalkMapState(), route)

        val command = completed.cameraCommand as MapCameraCommand.FitBounds
        assertEquals(51.99, command.bounds.minLatitude, 0.0)
        assertEquals(52.02, command.bounds.maxLatitude, 0.0)
    }

    @Test
    fun completingASinglePointRouteCentersAtWalkingZoom() {
        val route = routeWithPoints(MapLatLng(52.0, 13.0))

        val completed = WalkMapCameraPolicy.onWalkCompleted(WalkMapState(), route)

        val command = completed.cameraCommand as MapCameraCommand.MoveTo
        assertEquals(MapLatLng(52.0, 13.0), command.target)
        assertEquals(WalkMapCameraPolicy.COMPLETED_ZOOM, command.zoom, 0.0)
    }

    @Test
    fun completingAnEmptyRouteKeepsTheCameraUntouched() {
        val completed = WalkMapCameraPolicy.onWalkCompleted(WalkMapState(), WalkRoute())

        assertNull(completed.cameraCommand)
        assertTrue(completed.completionCameraApplied)
    }

    @Test
    fun completionCameraFiresOnlyOncePerWalk() {
        val route = routeWithPoints(MapLatLng(52.0, 13.0), MapLatLng(52.01, 13.01))
        val first = WalkMapCameraPolicy.onWalkCompleted(WalkMapState(), route)

        val second = WalkMapCameraPolicy.onWalkCompleted(first, route)

        assertSame(first.cameraCommand, second.cameraCommand)
    }

    @Test
    fun startingANewWalkResetsTheCameraButKeepsMapLoadingState() {
        val state = WalkMapCameraPolicy.onStyleLoaded(
            WalkMapCameraPolicy.onFix(WalkMapState(), MapLatLng(52.0, 13.0)),
        )

        val restarted = WalkMapCameraPolicy.onWalkStarting(state)

        assertNull(restarted.cameraCommand)
        assertNull(restarted.lastFix)
        assertNull(restarted.appliedFix)
        assertTrue(restarted.followEnabled)
        assertFalse(restarted.completionCameraApplied)
        assertTrue(restarted.styleLoaded)
        assertEquals(0L, restarted.styleGeneration)
    }

    private fun routeWithPoints(vararg points: MapLatLng): WalkRoute =
        points.foldIndexed(WalkRoute()) { index, route, location ->
            route.withPoint(point(index, location))
        }

    private fun point(sequence: Int, location: MapLatLng): WalkingLocationPoint =
        WalkingLocationPoint(
            sessionId = 1L,
            sequence = sequence,
            latitude = location.latitude,
            longitude = location.longitude,
            timestampMillis = 1_000L + sequence,
        )
}
