package com.waylo.app.domain.model

data class WorkoutStatistics(
    val distanceMeters: Double = 0.0,
    val activeMillis: Long = 0L,
    val averagePaceSecondsPerKm: Double? = null,
    val currentPaceSecondsPerKm: Double? = null,
    val averageSpeedMetersPerSecond: Double? = null,
    val currentSpeedMetersPerSecond: Double? = null,
    val estimatedCaloriesKcal: Double? = null,
    val walkSteps: Long? = null,
)
