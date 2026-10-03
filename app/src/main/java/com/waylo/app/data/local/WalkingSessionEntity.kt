package com.waylo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walking_sessions")
data class WalkingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val state: String,
    val startMillis: Long,
    val updatedMillis: Long,
    val distanceMeters: Double,
    val activeMillis: Long,
    val pausedMillis: Long,
    val activeSegmentStartMillis: Long?,
    val pausedSegmentStartMillis: Long?,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val errorMessage: String?,
    val walkStartStepCount: Long? = null,
    val walkStepCount: Long? = null,
)
