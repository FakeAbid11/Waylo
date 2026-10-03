package com.waylo.app.domain.model

data class WalkRoute(
    val points: List<WalkingLocationPoint> = emptyList(),
    val breakBeforeIndexes: Set<Int> = emptySet(),
) {
    val isEmpty: Boolean
        get() = points.isEmpty()

    fun withPoint(point: WalkingLocationPoint): WalkRoute = copy(points = points + point)

    fun markBreakBeforeNextPoint(): WalkRoute =
        if (points.isEmpty()) this else copy(breakBeforeIndexes = breakBeforeIndexes + points.size)

    fun latestOrNull(): WalkingLocationPoint? = points.lastOrNull()

    fun segments(): List<List<WalkingLocationPoint>> {
        if (points.isEmpty()) return emptyList()
        val segments = mutableListOf<MutableList<WalkingLocationPoint>>()
        points.forEachIndexed { index, point ->
            if (index == 0 || index in breakBeforeIndexes) {
                segments.add(mutableListOf(point))
            } else {
                segments.last().add(point)
            }
        }
        return segments
    }

    fun boundsOrNull(): RouteBounds? {
        if (points.isEmpty()) return null
        var minLatitude = points.first().latitude
        var maxLatitude = minLatitude
        var minLongitude = points.first().longitude
        var maxLongitude = minLongitude
        points.forEach { point ->
            if (point.latitude < minLatitude) minLatitude = point.latitude
            if (point.latitude > maxLatitude) maxLatitude = point.latitude
            if (point.longitude < minLongitude) minLongitude = point.longitude
            if (point.longitude > maxLongitude) maxLongitude = point.longitude
        }
        return RouteBounds(
            minLatitude = minLatitude,
            minLongitude = minLongitude,
            maxLatitude = maxLatitude,
            maxLongitude = maxLongitude,
        )
    }
}

data class RouteBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
) {
    val centerLatitude: Double
        get() = (minLatitude + maxLatitude) / 2.0

    val centerLongitude: Double
        get() = (minLongitude + maxLongitude) / 2.0

    val hasArea: Boolean
        get() = maxLatitude > minLatitude || maxLongitude > minLongitude
}
