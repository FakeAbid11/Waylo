package com.waylo.app.domain.model

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
) {
    val hasValidCoordinates: Boolean
        get() = latitude.isFinite() &&
            longitude.isFinite() &&
            latitude >= -90.0 && latitude <= 90.0 &&
            longitude >= -180.0 && longitude <= 180.0 &&
            timestampMillis > 0L
}
