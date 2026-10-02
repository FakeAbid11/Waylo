package com.waylo.app.data.local

import androidx.room.Entity

@Entity(tableName = "walking_location_points", primaryKeys = ["sessionId", "sequence"])
data class WalkingLocationPointEntity(
    val sessionId: Long,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMps: Float?,
)
