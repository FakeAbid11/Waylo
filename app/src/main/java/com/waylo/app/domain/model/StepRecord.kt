package com.waylo.app.domain.model

data class StepRecord(
    val epochDay: Long? = null,
    val baselineSensorCount: Long? = null,
    val lastSensorCount: Long? = null,
    val todaySteps: Long = 0,
)
