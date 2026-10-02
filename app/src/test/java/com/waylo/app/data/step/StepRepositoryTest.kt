package com.waylo.app.data.step

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.domain.model.StepStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StepRepositoryTest {

    private class FakeStepSensorDataSource(
        var available: Boolean = true,
    ) : StepSensorDataSource {
        var listener: ((Long) -> Unit)? = null
        var startCalls = 0
        var stopCalls = 0
        var registrationFails = false

        override fun isAvailable(): Boolean = available

        override fun start(onSensorValue: (Long) -> Unit): Boolean {
            startCalls += 1
            if (registrationFails || !available) return false
            listener = onSensorValue
            return true
        }

        override fun stop() {
            stopCalls += 1
            listener = null
        }

        fun emit(rawValue: Long) {
            listener?.invoke(rawValue)
        }
    }

    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var store: StepStateStore
    private lateinit var testScope: CoroutineScope
    private lateinit var sensor: FakeStepSensorDataSource
    private var today = 1_000L
    private var permission = PermissionState.Granted

    @Before
    fun setUp() {
        val file = File.createTempFile("waylo_repo_test", ".preferences_pb").apply { delete() }
        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes.add(storeScope)
        store = StepStateStore(PreferenceDataStoreFactory.create(scope = storeScope) { file })
        testScope = CoroutineScope(Dispatchers.Unconfined)
        scopes.add(testScope)
        sensor = FakeStepSensorDataSource()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun freshInstallReportsZeroStepsAndDefaultGoal() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        val state = repository.state.value
        assertEquals(0L, state.steps)
        assertEquals(6_000L, state.goal)
        assertTrue(state.sensorAvailable)
        assertEquals(PermissionState.Granted, state.permissionState)
        assertTrue(state.isTracking)
        assertEquals(StepStatus.Active, state.status)
    }

    @Test
    fun sensorReadingsUpdateStepsFromBaseline() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        sensor.emit(10_000L)
        sensor.emit(12_500L)

        assertEquals(2_500L, repository.state.value.steps)
    }

    @Test
    fun deniedPermissionNeverRegistersTheSensor() {
        permission = PermissionState.Denied
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        assertEquals(0, sensor.startCalls)
        assertFalse(repository.state.value.isTracking)
        assertEquals(StepStatus.PermissionNeeded, repository.state.value.status)
    }

    @Test
    fun missingSensorIsReportedHonestly() {
        sensor.available = false
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        assertEquals(0, sensor.startCalls)
        assertFalse(repository.state.value.isTracking)
        assertEquals(StepStatus.SensorUnavailable, repository.state.value.status)
    }

    @Test
    fun refreshAfterPermissionGrantRegistersTheSensor() {
        permission = PermissionState.Denied
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)
        assertEquals(0, sensor.startCalls)

        permission = PermissionState.Granted
        repository.refresh()

        assertEquals(1, sensor.startCalls)
        assertTrue(repository.state.value.isTracking)
        assertEquals(StepStatus.Active, repository.state.value.status)
    }

    @Test
    fun unsupportedPermissionStillTracksWithoutRequesting() {
        permission = PermissionState.Unsupported
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        assertTrue(repository.state.value.isTracking)
        assertEquals(StepStatus.Active, repository.state.value.status)
    }

    @Test
    fun registrationFailureLeavesTrackingStopped() {
        sensor.registrationFails = true
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        assertEquals(1, sensor.startCalls)
        assertFalse(repository.state.value.isTracking)
    }

    @Test
    fun dayRolloverResetsStepsAndKeepsCountingFromYesterday() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)
        sensor.emit(10_000L)
        sensor.emit(12_000L)
        assertEquals(2_000L, repository.state.value.steps)

        today = 1_001L
        repository.refresh()

        assertEquals(0L, repository.state.value.steps)
        sensor.emit(12_100L)
        assertEquals(100L, repository.state.value.steps)
    }

    @Test
    fun stopPersistsStepsAndUnregistersSensor() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)
        sensor.emit(10_000L)
        sensor.emit(12_000L)

        repository.stop()

        assertEquals(1, sensor.stopCalls)
        assertEquals(null, sensor.listener)
        assertFalse(repository.state.value.isTracking)
        awaitPersistedSteps(2_000L)
    }

    @Test
    fun restartRestoresPersistedSteps() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)
        sensor.emit(10_000L)
        sensor.emit(12_000L)
        repository.stop()
        awaitPersistedSteps(2_000L)

        sensor = FakeStepSensorDataSource()
        val relaunched = createRepository()
        relaunched.start()
        awaitLoaded(relaunched)

        assertEquals(2_000L, relaunched.state.value.steps)
    }

    @Test
    fun persistenceIsThrottledUntilTwentySensorEvents() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        sensor.emit(10_000L)
        for (step in 1L..18L) {
            sensor.emit(10_000L + step)
        }

        assertEquals(18L, repository.state.value.steps)
        assertEquals(0L, persistedSteps())

        sensor.emit(10_019L)

        assertEquals(19L, repository.state.value.steps)
        awaitPersistedSteps(19L)
    }

    @Test
    fun validGoalIsPersistedAndPublished() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        runBlocking { repository.setDailyGoal(8_000L) }

        assertEquals(8_000L, repository.state.value.goal)
        assertEquals(8_000L, runBlocking { store.snapshot.first().goal })
    }

    @Test
    fun invalidGoalsAreRejectedWithoutChanges() {
        val repository = createRepository()
        repository.start()
        awaitLoaded(repository)

        runBlocking { repository.setDailyGoal(500L) }
        runBlocking { repository.setDailyGoal(200_000L) }

        assertEquals(6_000L, repository.state.value.goal)
        assertEquals(6_000L, runBlocking { store.snapshot.first().goal })
    }

    private fun createRepository(): StepRepositoryImpl = StepRepositoryImpl(
        sensor = sensor,
        store = store,
        permissionState = { permission },
        todayEpochDay = { today },
        scope = testScope,
    )

    private fun awaitLoaded(repository: StepRepository) = runBlocking {
        withTimeout(5_000) {
            repository.state.first { it.isLoaded }
        }
    }

    private fun awaitPersistedSteps(expected: Long) = runBlocking {
        withTimeout(5_000) {
            store.snapshot.first { it.record.todaySteps == expected }
        }
    }

    private fun persistedSteps(): Long = runBlocking {
        withTimeout(5_000) {
            store.snapshot.first().record.todaySteps
        }
    }
}
