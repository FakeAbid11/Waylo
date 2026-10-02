package com.waylo.app.data.step

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.waylo.app.domain.model.DailyGoalValidator
import com.waylo.app.domain.model.StepRecord
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StepStateStoreTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun freshInstallUsesDefaults() = runBlocking {
        val store = createStore()

        val snapshot = store.snapshot.first()

        assertEquals(DailyGoalValidator.DEFAULT_STEPS, snapshot.goal)
        assertEquals(StepRecord(), snapshot.record)
    }

    @Test
    fun goalPersistsAcrossRestarts() = runBlocking {
        val file = storeFile()
        val firstLaunch = createStore(file)

        firstLaunch.setGoal(8_000L)

        scopes.first().cancel()
        delay(500)

        val relaunch = createStore(file)
        assertEquals(8_000L, relaunch.snapshot.first().goal)
    }

    @Test
    fun recordPersistsAcrossRestarts() = runBlocking {
        val file = storeFile()
        val firstLaunch = createStore(file)
        val record = StepRecord(
            epochDay = 200L,
            baselineSensorCount = 10_000L,
            lastSensorCount = 10_250L,
            todaySteps = 250L,
        )

        firstLaunch.setRecord(record)

        scopes.first().cancel()
        delay(500)

        val relaunch = createStore(file)
        assertEquals(record, relaunch.snapshot.first().record)
    }

    @Test
    fun clearedRecordFieldsAreRemovedFromStorage() = runBlocking {
        val store = createStore()
        store.setRecord(
            StepRecord(
                epochDay = 200L,
                baselineSensorCount = 10_000L,
                lastSensorCount = 10_250L,
                todaySteps = 250L,
            ),
        )

        store.setRecord(StepRecord(epochDay = 201L))

        val record = store.snapshot.first().record
        assertEquals(201L, record.epochDay)
        assertEquals(null, record.baselineSensorCount)
        assertEquals(null, record.lastSensorCount)
        assertEquals(0L, record.todaySteps)
    }

    @Test
    fun negativeStoredStepsAreClampedToZero() = runBlocking {
        val store = createStore()

        store.setRecord(StepRecord(epochDay = 1L, todaySteps = -7L))

        assertEquals(0L, store.snapshot.first().record.todaySteps)
    }

    private fun storeFile(): File = File(
        RuntimeEnvironment.getApplication().filesDir,
        "waylo_steps_test_${System.nanoTime()}.preferences_pb",
    ).apply { delete() }

    private fun createStore(file: File = storeFile()): StepStateStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes.add(scope)
        return StepStateStore(
            PreferenceDataStoreFactory.create(scope = scope) { file },
        )
    }
}
