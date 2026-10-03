package com.waylo.app.domain.model

enum class WalkingState {
    Idle,
    Starting,
    Active,
    Paused,
    Stopping,
    Completed,
    Error;

    val isOngoing: Boolean
        get() = this == Starting || this == Active || this == Paused
}

data class WalkingStatus(
    val state: WalkingState = WalkingState.Idle,
    val sessionId: Long? = null,
    val distanceMeters: Double = 0.0,
    val activeMillis: Long = 0L,
    val activeSegmentStartMillis: Long? = null,
    val startedAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val hasFix: Boolean = false,
    val errorMessage: String? = null,
    val walkStartStepCount: Long? = null,
    val walkStepCount: Long? = null,
) {
    fun activeMillisAt(nowMillis: Long): Long {
        val segmentStart = activeSegmentStartMillis
        val running = if (state == WalkingState.Active && segmentStart != null) {
            (nowMillis - segmentStart).coerceAtLeast(0L)
        } else {
            0L
        }
        return activeMillis + running
    }
}
