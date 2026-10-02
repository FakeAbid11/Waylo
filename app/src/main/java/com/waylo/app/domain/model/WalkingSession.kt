package com.waylo.app.domain.model

data class WalkingSession(
    val id: Long = 0L,
    val state: WalkingState,
    val startMillis: Long,
    val updatedMillis: Long,
    val distanceMeters: Double = 0.0,
    val activeMillis: Long = 0L,
    val pausedMillis: Long = 0L,
    val activeSegmentStartMillis: Long? = null,
    val pausedSegmentStartMillis: Long? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val errorMessage: String? = null,
) {
    val hasFix: Boolean
        get() = lastLatitude != null && lastLongitude != null
}
