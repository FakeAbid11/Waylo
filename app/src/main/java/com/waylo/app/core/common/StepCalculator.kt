package com.waylo.app.core.common

import com.waylo.app.domain.model.StepRecord

internal fun rollToDay(record: StepRecord, todayEpochDay: Long): StepRecord {
    val recordedDay = record.epochDay ?: return record.copy(epochDay = todayEpochDay)
    if (recordedDay >= todayEpochDay) return record

    val daysGone = todayEpochDay - recordedDay
    val anchor = if (daysGone == 1L) record.lastSensorCount else null
    return record.copy(
        epochDay = todayEpochDay,
        baselineSensorCount = anchor,
        todaySteps = 0,
    )
}

internal fun applySensorValue(record: StepRecord, rawSensorValue: Long, todayEpochDay: Long): StepRecord {
    val rolled = rollToDay(record, todayEpochDay)
    val baseline = rolled.baselineSensorCount
    if (baseline == null || rawSensorValue < baseline) {
        return rolled.copy(
            baselineSensorCount = rawSensorValue,
            lastSensorCount = rawSensorValue,
            todaySteps = 0,
        )
    }
    val last = rolled.lastSensorCount
    if (last != null && rawSensorValue < last) return rolled
    return rolled.copy(
        lastSensorCount = rawSensorValue,
        todaySteps = rawSensorValue - baseline,
    )
}

internal fun sanitizeRecord(record: StepRecord): StepRecord {
    val baseline = record.baselineSensorCount ?: record.lastSensorCount
    val steps = if (baseline == null) 0 else record.todaySteps.coerceAtLeast(0)
    return record.copy(
        baselineSensorCount = baseline,
        lastSensorCount = record.lastSensorCount ?: baseline,
        todaySteps = steps,
    )
}
