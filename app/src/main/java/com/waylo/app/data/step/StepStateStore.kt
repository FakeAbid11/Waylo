package com.waylo.app.data.step

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.waylo.app.domain.model.DailyGoalValidator
import com.waylo.app.domain.model.StepRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class StepSnapshot(
    val goal: Long,
    val record: StepRecord,
)

class StepStateStore(
    private val dataStore: DataStore<Preferences>,
) {

    val snapshot: Flow<StepSnapshot> = dataStore.data.map { preferences ->
        StepSnapshot(
            goal = preferences[KEY_DAILY_STEP_GOAL] ?: DailyGoalValidator.DEFAULT_STEPS,
            record = StepRecord(
                epochDay = preferences[KEY_STEP_DAY],
                baselineSensorCount = preferences[KEY_STEP_BASELINE],
                lastSensorCount = preferences[KEY_STEP_LAST_SENSOR],
                todaySteps = preferences[KEY_STEP_TODAY] ?: 0L,
            ),
        )
    }

    suspend fun setGoal(goal: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_STEP_GOAL] = goal
        }
    }

    suspend fun setRecord(record: StepRecord) {
        dataStore.edit { preferences ->
            preferences.putOrRemove(KEY_STEP_DAY, record.epochDay)
            preferences.putOrRemove(KEY_STEP_BASELINE, record.baselineSensorCount)
            preferences.putOrRemove(KEY_STEP_LAST_SENSOR, record.lastSensorCount)
            preferences[KEY_STEP_TODAY] = record.todaySteps.coerceAtLeast(0)
        }
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<Long>, value: Long?) {
        if (value == null) remove(key) else set(key, value)
    }

    private companion object {
        val KEY_DAILY_STEP_GOAL = longPreferencesKey("daily_step_goal")
        val KEY_STEP_DAY = longPreferencesKey("step_day")
        val KEY_STEP_BASELINE = longPreferencesKey("step_baseline")
        val KEY_STEP_LAST_SENSOR = longPreferencesKey("step_last_sensor")
        val KEY_STEP_TODAY = longPreferencesKey("step_today")
    }
}
