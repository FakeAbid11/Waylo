package com.waylo.app.data.map

import com.waylo.app.domain.model.RouteBounds
import com.waylo.app.domain.model.WalkRoute
import kotlin.math.cos
import kotlin.math.sqrt

enum class WalkMapLoadState {
    Loading,
    Ready,
    Unavailable,
    StyleError,
}

sealed interface MapCameraCommand {
    val id: Long

    data class MoveTo(
        override val id: Long,
        val target: MapLatLng,
        val zoom: Double,
    ) : MapCameraCommand

    data class FitBounds(
        override val id: Long,
        val bounds: RouteBounds,
    ) : MapCameraCommand
}

data class WalkMapState(
    val networkAvailable: Boolean = true,
    val styleLoaded: Boolean = false,
    val styleFailed: Boolean = false,
    val styleGeneration: Long = 0L,
    val followEnabled: Boolean = true,
    val lastFix: MapLatLng? = null,
    val appliedFix: MapLatLng? = null,
    val cameraCommand: MapCameraCommand? = null,
    val commandCounter: Long = 0L,
    val completionCameraApplied: Boolean = false,
) {
    val loadState: WalkMapLoadState
        get() = when {
            !networkAvailable -> WalkMapLoadState.Unavailable
            !styleLoaded -> if (styleFailed) WalkMapLoadState.StyleError else WalkMapLoadState.Loading
            else -> WalkMapLoadState.Ready
        }
}

object WalkMapCameraPolicy {
    const val WALKING_ZOOM = 16.5
    const val COMPLETED_ZOOM = 16.0
    const val FOLLOW_MIN_DISTANCE_METERS = 8.0
    const val CAMERA_PADDING_PIXELS = 96

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun onStyleLoaded(state: WalkMapState): WalkMapState =
        state.copy(styleLoaded = true, styleFailed = false)

    fun onStyleFailed(state: WalkMapState): WalkMapState =
        if (state.styleLoaded) state else state.copy(styleFailed = true)

    fun onNetworkChanged(state: WalkMapState, available: Boolean): WalkMapState {
        val restored = state.copy(networkAvailable = available)
        if (available && !state.networkAvailable && !state.styleLoaded) {
            return restored.copy(
                styleFailed = false,
                styleGeneration = restored.styleGeneration + 1,
            )
        }
        return restored
    }

    fun onRetryStyle(state: WalkMapState): WalkMapState = state.copy(
        styleLoaded = false,
        styleFailed = false,
        styleGeneration = state.styleGeneration + 1,
    )

    fun onMapPan(state: WalkMapState): WalkMapState =
        if (state.followEnabled) state.copy(followEnabled = false) else state

    fun onFix(state: WalkMapState, fix: MapLatLng): WalkMapState {
        val withFix = state.copy(lastFix = fix)
        if (!state.followEnabled) return withFix
        val applied = state.appliedFix
        if (applied != null && distanceMeters(applied, fix) < FOLLOW_MIN_DISTANCE_METERS) {
            return withFix
        }
        return emitCommand(
            withFix.copy(appliedFix = fix),
            MapCameraCommand.MoveTo(id = 0L, target = fix, zoom = WALKING_ZOOM),
        )
    }

    fun onRecenter(state: WalkMapState): WalkMapState {
        val fix = state.lastFix ?: return state
        return emitCommand(
            state.copy(followEnabled = true, appliedFix = fix),
            MapCameraCommand.MoveTo(id = 0L, target = fix, zoom = WALKING_ZOOM),
        )
    }

    fun onWalkCompleted(state: WalkMapState, route: WalkRoute): WalkMapState {
        if (state.completionCameraApplied) return state
        val bounds = route.boundsOrNull()
            ?: return state.copy(completionCameraApplied = true)
        val command = if (bounds.hasArea) {
            MapCameraCommand.FitBounds(id = 0L, bounds = bounds)
        } else {
            MapCameraCommand.MoveTo(
                id = 0L,
                target = MapLatLng(bounds.centerLatitude, bounds.centerLongitude),
                zoom = COMPLETED_ZOOM,
            )
        }
        return emitCommand(
            state.copy(followEnabled = false, completionCameraApplied = true),
            command,
        )
    }

    fun onWalkStarting(state: WalkMapState): WalkMapState = WalkMapState(
        networkAvailable = state.networkAvailable,
        styleLoaded = state.styleLoaded,
        styleFailed = state.styleFailed,
        styleGeneration = state.styleGeneration,
    )

    private fun emitCommand(state: WalkMapState, command: MapCameraCommand): WalkMapState {
        val nextId = state.commandCounter + 1
        return state.copy(
            cameraCommand = when (command) {
                is MapCameraCommand.MoveTo -> command.copy(id = nextId)
                is MapCameraCommand.FitBounds -> command.copy(id = nextId)
            },
            commandCounter = nextId,
        )
    }

    private fun distanceMeters(from: MapLatLng, to: MapLatLng): Double {
        val meanLatitude = Math.toRadians((from.latitude + to.latitude) / 2.0)
        val east = Math.toRadians(to.longitude - from.longitude) * cos(meanLatitude) * EARTH_RADIUS_METERS
        val north = Math.toRadians(to.latitude - from.latitude) * EARTH_RADIUS_METERS
        return sqrt(east * east + north * north)
    }
}
