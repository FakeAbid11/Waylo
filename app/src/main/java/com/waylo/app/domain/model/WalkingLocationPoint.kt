package com.waylo.app.domain.model

data class WalkingLocationPoint(
    val sessionId: Long,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
)
