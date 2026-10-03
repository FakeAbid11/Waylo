package com.waylo.app.data.step

import com.waylo.app.core.common.applySensorValue
import com.waylo.app.core.common.rollToDay
import com.waylo.app.core.common.sanitizeRecord
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.domain.model.DailyGoalValidator
import com.waylo.app.domain.model.DailyStepState
import com.waylo.app.domain.model.StepRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface StepRepository {
    val state: StateFlow<DailyStepState>
    fun start()
    fun refresh()
    fun stop()
    fun currentSensorCount(): Long?
    suspend fun setDailyGoal(goal: Long)
}

class StepRepositoryImpl(
    private val sensor: StepSensorDataSource,
    private val store: StepStateStore,
    private val permissionState: () -> PermissionState,
    private val todayEpochDay: () -> Long,
    private val scope: CoroutineScope,
) : StepRepository {

    private val _state = MutableStateFlow(DailyStepState())
    override val state: StateFlow<DailyStepState> = _state.asStateFlow()

    private var record: StepRecord = StepRecord()
    private var started = false
    private var loaded = false
    private var eventsSincePersist = 0

    override fun start() {
        if (started) return
        started = true
        scope.launch {
            val snapshot = store.snapshot.first()
            val sanitized = sanitizeRecord(snapshot.record)
            record = rollToDay(sanitized, todayEpochDay())
            loaded = true
            val tracking = registerSensor()
            if (record != sanitized) store.setRecord(record)
            _state.value = DailyStepState(
                steps = record.todaySteps,
                goal = snapshot.goal,
                sensorAvailable = sensor.isAvailable(),
                permissionState = permissionState(),
                isTracking = tracking,
                isLoaded = true,
                lastSensorCount = record.lastSensorCount?.takeIf { it > 0L },
            )
        }
    }

    override fun refresh() {
        if (!started) {
            start()
            return
        }
        if (!loaded) return
        scope.launch {
            val previousDay = record.epochDay
            record = rollToDay(record, todayEpochDay())
            val dayChanged = record.epochDay != previousDay
            val tracking = registerSensor()
            _state.value = _state.value.copy(
                steps = record.todaySteps,
                sensorAvailable = sensor.isAvailable(),
                permissionState = permissionState(),
                isTracking = tracking,
                isLoaded = true,
                lastSensorCount = record.lastSensorCount?.takeIf { it > 0L },
            )
            if (dayChanged) store.setRecord(record)
        }
    }

    override fun stop() {
        if (!started) return
        started = false
        loaded = false
        sensor.stop()
        _state.value = _state.value.copy(isTracking = false)
        val recordToPersist = record
        scope.launch { store.setRecord(recordToPersist) }
    }

    override fun currentSensorCount(): Long? = _state.value.lastSensorCount

    override suspend fun setDailyGoal(goal: Long) {
        if (goal < DailyGoalValidator.MIN_STEPS || goal > DailyGoalValidator.MAX_STEPS) return
        store.setGoal(goal)
        _state.value = _state.value.copy(goal = goal)
    }

    private fun registerSensor(): Boolean {
        val permission = permissionState()
        val allowed = permission == PermissionState.Granted || permission == PermissionState.Unsupported
        if (!allowed || !sensor.isAvailable()) {
            sensor.stop()
            return false
        }
        return sensor.start { rawValue ->
            scope.launch { handleSensorValue(rawValue) }
        }
    }

    private suspend fun handleSensorValue(rawValue: Long) {
        if (!loaded) return
        val previousDay = record.epochDay
        record = applySensorValue(record, rawValue, todayEpochDay())
        val dayChanged = record.epochDay != previousDay
        _state.value = _state.value.copy(
            steps = record.todaySteps,
            lastSensorCount = record.lastSensorCount?.takeIf { it > 0L },
        )
        eventsSincePersist += 1
        if (dayChanged || eventsSincePersist >= PERSIST_EVERY_N_EVENTS) {
            store.setRecord(record)
            eventsSincePersist = 0
        }
    }

    private companion object {
        const val PERSIST_EVERY_N_EVENTS = 20
    }
}
