package com.waylo.app.ui.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.viewinterop.AndroidView
import com.waylo.app.data.map.MapRoutePresentation
import com.waylo.app.data.map.MapStyleConfiguration
import com.waylo.app.data.map.WalkMapCameraPolicy
import com.waylo.app.data.map.WalkMapState
import com.waylo.app.domain.model.WalkRoute
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

@Composable
fun WalkMap(
    mapState: WalkMapState,
    route: WalkRoute,
    onStyleLoaded: () -> Unit,
    onStyleFailed: () -> Unit,
    onPanGesture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Color(0xFF0B1220)))
        return
    }

    val holder = rememberMapHolder()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            if (!MapLibre.hasInstance()) {
                MapLibre.getInstance(context)
            }
            val mapView = MapView(context)
            holder.mapView = mapView
            mapView.onCreate(null)
            mapView.addOnDidFinishLoadingStyleListener { holder.handleStyleLoaded() }
            mapView.addOnDidFailLoadingMapListener { holder.handleStyleFailed() }
            mapView.getMapAsync { map ->
                holder.map = map
                map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        holder.onPanGesture?.invoke()
                    }

                    override fun onMove(detector: MoveGestureDetector) = Unit

                    override fun onMoveEnd(detector: MoveGestureDetector) = Unit
                })
                map.setStyle(MapStyleConfiguration.defaultStyleUri())
            }
            mapView
        },
        update = { _ ->
            holder.mapState = mapState
            holder.route = route
            holder.onStyleLoaded = onStyleLoaded
            holder.onStyleFailed = onStyleFailed
            holder.onPanGesture = onPanGesture
            holder.applyPending()
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> holder.mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> holder.mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.mapView?.onPause()
                Lifecycle.Event.ON_STOP -> holder.mapView?.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.destroy()
        }
    }
}

@Composable
private fun rememberMapHolder(): MapHolder = remember { MapHolder() }

private class MapHolder {
    var mapView: MapView? = null
    var map: MapLibreMap? = null
    var mapState: WalkMapState = WalkMapState()
    var route: WalkRoute = WalkRoute()
    var onStyleLoaded: (() -> Unit)? = null
    var onStyleFailed: (() -> Unit)? = null
    var onPanGesture: (() -> Unit)? = null

    private var appliedCommandId: Long? = null
    private var appliedStyleGeneration: Long = 0L
    private var lastRouteJson: String? = null
    private var lastMarkerJson: String? = null
    private var destroyed = false

    fun handleStyleLoaded() {
        val map = map ?: return
        map.getStyle { style ->
            setupLayers(style)
            lastRouteJson = null
            lastMarkerJson = null
            applyPending()
        }
        onStyleLoaded?.invoke()
    }

    fun handleStyleFailed() {
        onStyleFailed?.invoke()
    }

    fun applyPending() {
        if (destroyed) return
        val map = map ?: return
        val style = map.style ?: return
        setupLayers(style)
        pushRouteData(style)
        pushMarkerData(style)
        applyStyleGeneration(map)
        applyCamera(map)
    }

    fun destroy() {
        val view = mapView ?: return
        destroyed = true
        if (!view.isDestroyed) {
            view.onPause()
            view.onStop()
            view.onDestroy()
        }
        mapView = null
        map = null
    }

    private fun setupLayers(style: org.maplibre.android.maps.Style) {
        if (style.getSource(ROUTE_SOURCE_ID) == null) {
            style.addSource(
                GeoJsonSource(ROUTE_SOURCE_ID, GeoJsonOptions().withLineMetrics(true)),
            )
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineGradient(routeGradient()),
                    PropertyFactory.lineWidth(ROUTE_WIDTH),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round"),
                    PropertyFactory.lineOpacity(0.95f),
                ),
            )
        }
        if (style.getSource(MARKER_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(MARKER_SOURCE_ID))
            style.addLayer(
                CircleLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                    PropertyFactory.circleRadius(MARKER_RADIUS),
                    PropertyFactory.circleColor("#22D3EE"),
                    PropertyFactory.circleStrokeWidth(MARKER_STROKE_WIDTH),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                ),
            )
        }
    }

    private fun pushRouteData(style: org.maplibre.android.maps.Style) {
        val desired = MapRoutePresentation.routeGeoJson(route)
        if (desired == lastRouteJson) return
        lastRouteJson = desired
        val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID) ?: return
        source.setGeoJson(desired ?: MapRoutePresentation.EMPTY_FEATURE_COLLECTION)
    }

    private fun pushMarkerData(style: org.maplibre.android.maps.Style) {
        val desired = MapRoutePresentation.markerGeoJson(route)
        if (desired == lastMarkerJson) return
        lastMarkerJson = desired
        val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID) ?: return
        source.setGeoJson(desired ?: MapRoutePresentation.EMPTY_FEATURE_COLLECTION)
    }

    private fun applyStyleGeneration(map: MapLibreMap) {
        val generation = mapState.styleGeneration
        if (generation == appliedStyleGeneration) return
        appliedStyleGeneration = generation
        if (generation > 0L) {
            lastRouteJson = null
            lastMarkerJson = null
            map.setStyle(MapStyleConfiguration.defaultStyleUri())
        }
    }

    private fun applyCamera(map: MapLibreMap) {
        val command = mapState.cameraCommand ?: return
        if (command.id == appliedCommandId) return
        appliedCommandId = command.id
        when (command) {
            is com.waylo.app.data.map.MapCameraCommand.MoveTo -> {
                map.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(command.target.latitude, command.target.longitude),
                        command.zoom,
                    ),
                )
            }

            is com.waylo.app.data.map.MapCameraCommand.FitBounds -> {
                val bounds = command.bounds
                val latLngBounds = LatLngBounds.Builder()
                    .include(LatLng(bounds.minLatitude, bounds.minLongitude))
                    .include(LatLng(bounds.maxLatitude, bounds.maxLongitude))
                    .build()
                val camera = map.getCameraForLatLngBounds(
                    latLngBounds,
                    IntArray(4) { WalkMapCameraPolicy.CAMERA_PADDING_PIXELS },
                ) ?: return
                map.easeCamera(CameraUpdateFactory.newCameraPosition(camera))
            }
        }
    }

    private fun routeGradient(): Expression = Expression.interpolate(
        Expression.linear(),
        Expression.lineProgress(),
        Expression.stop(0.0, "#22D3EE"),
        Expression.stop(0.5, "#3B82F6"),
        Expression.stop(1.0, "#8B5CF6"),
    )

    private companion object {
        const val ROUTE_SOURCE_ID = "waylo-route"
        const val ROUTE_LAYER_ID = "waylo-route-line"
        const val MARKER_SOURCE_ID = "waylo-position"
        const val MARKER_LAYER_ID = "waylo-position-circle"
        const val ROUTE_WIDTH = 6f
        const val MARKER_RADIUS = 9f
        const val MARKER_STROKE_WIDTH = 3f
    }
}
