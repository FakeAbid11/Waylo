package com.waylo.app.data.map

import com.waylo.app.domain.model.WalkRoute

object MapRoutePresentation {

    const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""

    fun routeGeoJson(route: WalkRoute): String? {
        val segments = route.segments()
        if (segments.isEmpty()) return null
        val coordinates = segments.joinToString(separator = ",") { segment ->
            segment.joinToString(separator = ",", prefix = "[", postfix = "]") { point ->
                "[${point.longitude},${point.latitude}]"
            }
        }
        return """{"type":"Feature","geometry":{"type":"MultiLineString","coordinates":[$coordinates]}}"""
    }

    fun markerGeoJson(route: WalkRoute): String? {
        val latest = route.latestOrNull() ?: return null
        return """{"type":"Feature","geometry":{"type":"Point","coordinates":[${latest.longitude},${latest.latitude}]}}"""
    }
}
