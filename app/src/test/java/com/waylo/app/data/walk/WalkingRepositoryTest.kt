package com.waylo.app.data.walk

import androidx.room.Room
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.local.WalkingLocationPointEntity
import com.waylo.app.data.local.WalkingSessionEntity
import com.waylo.app.data.location.LocationDataSource
import com.waylo.app.domain.model.LocationSample
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.math.cos
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
class WalkingRepositoryTest {

    private class FakeLocationDataSource(
        var enabled: Boolean = true,
        var startSucceeds: Boolean = true,
    ) : LocationDataSource {
        var listener: ((LocationSample) -> Unit)? = null
        var startCalls = 0
        var stopCalls = 0

        override fun isLocationEnabled(): Boolean = enabled

        override fun start(onSample: (LocationSample) -> Unit): Boolean {
            startCalls += 1
            if (!startSucceeds || !enabled) return false
            listener = onSample
            return true
        }

        override fun stop() {
            stopCalls += 1
            listener = null
        }

        fun emit(sample: LocationSample) {
            listener?.invoke(sample)
        }
    }

    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var database: WayloDatabase
    private lateinit var testScope: CoroutineScope
    private lateinit var location: FakeLocationDataSource
    private lateinit var repository: WalkingRepositoryImpl
    private var permission = PermissionState.Granted
    private var currentTime = T0
    private var sensorCount: Long? = null

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()
        testScope = CoroutineScope(Dispatchers.Unconfined)
        scopes.add(testScope)
        location = FakeLocationDataSource()
        permission = PermissionState.Granted
        currentTime = T0
        sensorCount = null
        repository = createRepository()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
        database.close()
    }

    @Test
    fun startWalkGoesActiveAndPersistsTheSession() {
        repository.startWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        assertEquals(WalkingState.Active, status.state)
        assertTrue(status.sessionId != null && status.sessionId > 0L)
        assertEquals(T0, status.activeSegmentStartMillis)
        assertTrue(location.startCalls >= 1)

        val row = runBlocking { database.walkingDao().sessionById(status.sessionId!!) }
        assertEquals("Active", row?.state)
    }

    @Test
    fun startWalkWithoutPermissionFailsHonestly() {
        permission = PermissionState.Denied

        repository.startWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Error }

        assertEquals("Location permission is needed to record this walk.", status.errorMessage)
        assertEquals(0, location.startCalls)
        settle()
        val row = runBlocking { database.walkingDao().latestSession() }
        assertEquals("Error", row?.state)
    }

    @Test
    fun startWalkWithDisabledLocationServicesFailsHonestly() {
        location.enabled = false

        repository.startWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Error }

        assertEquals("Location services are turned off.", status.errorMessage)
        assertEquals(0, location.startCalls)
        settle()
    }

    @Test
    fun startWalkWhenTheProviderRefusesFailsHonestly() {
        location.startSucceeds = false

        repository.startWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Error }

        assertEquals("Location isn't available on this device.", status.errorMessage)
        assertEquals(1, location.startCalls)
        settle()
    }

    @Test
    fun duplicateStartDoesNotCreateASecondSession() {
        repository.startWalk()
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        assertEquals(1, runBlocking { database.walkingDao().sessionCount() })
        assertEquals(1, location.startCalls)
    }

    @Test
    fun acceptedSamplesAccumulateDistanceAndPersistPoints() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }

        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        val fixed = awaitStatus(repository) { it.hasFix }

        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        val walked = awaitStatus(repository) { it.distanceMeters > 50.0 }
        settle()

        assertEquals(111.19, walked.distanceMeters, 0.05)
        assertEquals(fixed.sessionId, walked.sessionId)

        val points = runBlocking { database.walkingDao().locationPoints(walked.sessionId!!) }
        assertEquals(2, points.size)

        val row = runBlocking { database.walkingDao().sessionById(walked.sessionId!!) }
        assertEquals(walked.distanceMeters, row!!.distanceMeters, 0.001)
    }

    @Test
    fun rejectedJumpKeepsDistanceAndSkipsThePoint() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 30_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 30_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        settle()

        currentTime = T0 + 31_000L
        location.emit(sample(latitude = 52.5, timestampMillis = T0 + 31_000L))
        settle()

        val status = repository.status.value
        assertEquals(111.19, status.distanceMeters, 0.05)
        val points = runBlocking { database.walkingDao().locationPoints(status.sessionId!!) }
        assertEquals(2, points.size)
    }

    @Test
    fun pauseResumeStopKeepsActiveTimeHonestAndResetsTheAnchor() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }

        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }

        repository.pauseWalk()
        val paused = awaitStatus(repository) { it.state == WalkingState.Paused }
        settle()

        assertEquals(60_000L, paused.activeMillis)
        assertNull(paused.activeSegmentStartMillis)
        assertNull(location.listener)

        location.emit(sample(latitude = 52.002, timestampMillis = T0 + 90_000L))
        settle()
        assertEquals(111.19, repository.status.value.distanceMeters, 0.05)

        currentTime = T0 + 180_000L
        repository.resumeWalk()
        val resumed = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        assertEquals(60_000L, resumed.activeMillis)
        assertEquals(T0 + 180_000L, resumed.activeSegmentStartMillis)

        location.emit(sample(latitude = 53.0, timestampMillis = T0 + 180_000L))
        settle()
        assertEquals(111.19, repository.status.value.distanceMeters, 0.05)

        currentTime = T0 + 240_000L
        location.emit(sample(latitude = 53.001, timestampMillis = T0 + 240_000L))
        val further = awaitStatus(repository) { it.distanceMeters > 150.0 }
        assertEquals(222.38, further.distanceMeters, 0.1)

        repository.stopWalk()
        val completed = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertEquals(120_000L, completed.activeMillis)
        assertEquals(222.38, completed.distanceMeters, 0.1)
        assertNull(completed.activeSegmentStartMillis)
        assertNull(location.listener)

        val row = runBlocking { database.walkingDao().sessionById(completed.sessionId!!) }
        assertEquals("Completed", row?.state)
        assertEquals(120_000L, row!!.pausedMillis)
        assertEquals(completed.distanceMeters, row.distanceMeters, 0.0)
    }

    @Test
    fun stopWhilePausedCompletesTheWalk() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        repository.pauseWalk()
        awaitStatus(repository) { it.state == WalkingState.Paused }

        repository.stopWalk()
        val completed = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertNull(completed.errorMessage)
        assertEquals(0L, completed.activeMillis)
        assertNull(location.listener)
    }

    @Test
    fun stopWhileIdleDoesNothing() {
        repository.stopWalk()
        settle()

        assertEquals(WalkingState.Idle, repository.status.value.state)
        assertEquals(0, runBlocking { database.walkingDao().sessionCount() })
    }

    @Test
    fun everyStateTransitionIsObservable() {
        val history = mutableListOf<WalkingState>()
        val collector = testScope.launchCollector(repository) { history.add(it) }

        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        allowCollectorToCatchUp()
        repository.pauseWalk()
        awaitStatus(repository) { it.state == WalkingState.Paused }
        allowCollectorToCatchUp()
        repository.resumeWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        allowCollectorToCatchUp()
        repository.stopWalk()
        awaitStatus(repository) { it.state == WalkingState.Completed }
        allowCollectorToCatchUp()
        collector.cancel()

        assertEquals(
            listOf(
                WalkingState.Idle,
                WalkingState.Starting,
                WalkingState.Active,
                WalkingState.Paused,
                WalkingState.Active,
                WalkingState.Stopping,
                WalkingState.Completed,
            ),
            history,
        )
    }

    @Test
    fun activeSessionRecoversAfterProcessDeathWithoutCountingTheDeadGap() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        settle()

        killScope()
        location.listener = null
        currentTime = T0 + 360_000L
        val recovered = createRepository()
        val status = awaitStatus(recovered) { it.state == WalkingState.Active }
        settle()

        assertEquals(111.19, status.distanceMeters, 0.05)
        assertEquals(60_000L, status.activeMillis)
        assertNull(status.activeSegmentStartMillis)

        recovered.attachService()
        val attached = awaitStatus(recovered) { it.activeSegmentStartMillis != null }
        settle()

        assertEquals(T0 + 360_000L, attached.activeSegmentStartMillis)
        assertTrue(location.listener != null)

        recovered.startWalk()
        settle()
        assertEquals(1, runBlocking { database.walkingDao().sessionCount() })
        assertEquals(2, location.startCalls)
    }

    @Test
    fun completedSessionSurvivesRestartWithoutResurrectingTheUi() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        repository.stopWalk()
        awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        killScope()
        val recovered = createRepository()
        settle()

        assertEquals(WalkingState.Idle, recovered.status.value.state)
        val row = runBlocking { database.walkingDao().latestSession() }
        assertEquals("Completed", row?.state)
        assertEquals(111.19, row!!.distanceMeters, 0.01)
    }

    @Test
    fun errorSessionIsRestoredAndCanBeRetriedOrFinished() {
        permission = PermissionState.Denied
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Error }
        settle()

        killScope()
        val recovered = createRepository()
        val restored = awaitStatus(recovered) { it.state == WalkingState.Error }
        assertEquals("Location permission is needed to record this walk.", restored.errorMessage)

        permission = PermissionState.Granted
        recovered.retryTracking()
        val retried = awaitStatus(recovered) { it.state == WalkingState.Active }
        assertEquals(WalkingState.Active, retried.state)

        recovered.stopWalk()
        val completed = awaitStatus(recovered) { it.state == WalkingState.Completed }
        settle()
        assertEquals(WalkingState.Completed, completed.state)
    }

    @Test
    fun unexpectedServiceDeathFailsWithTheDistanceStillSalvageable() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }

        repository.detachService()
        val failed = awaitStatus(repository) { it.state == WalkingState.Error }
        settle()

        assertEquals("Walk tracking was interrupted.", failed.errorMessage)
        assertEquals(111.19, failed.distanceMeters, 0.05)
        assertNull(location.listener)

        repository.stopWalk()
        val completed = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertEquals(111.19, completed.distanceMeters, 0.05)
        assertNull(completed.errorMessage)
    }

    @Test
    fun interruptedStoppingRowIsFinalizedOnRestart() {
        runBlocking {
            database.walkingDao().insertSession(
                WalkingSessionEntity(
                    state = "Stopping",
                    startMillis = T0,
                    updatedMillis = T0,
                    distanceMeters = 42.0,
                    activeMillis = 1_000L,
                    pausedMillis = 0L,
                    activeSegmentStartMillis = null,
                    pausedSegmentStartMillis = null,
                    startLatitude = null,
                    startLongitude = null,
                    lastLatitude = null,
                    lastLongitude = null,
                    errorMessage = null,
                ),
            )
        }

        val recovered = createRepository()
        settle()

        assertEquals(WalkingState.Idle, recovered.status.value.state)
        val row = runBlocking { database.walkingDao().latestSession() }
        assertEquals("Completed", row?.state)
        assertEquals(42.0, row!!.distanceMeters, 0.001)
    }

    @Test
    fun attachingAnAlreadyTrackingServiceDoesNotRestartUpdates() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        repository.attachService()
        settle()

        assertEquals(1, location.startCalls)
        assertEquals(WalkingState.Active, repository.status.value.state)
    }

    @Test
    fun reportErrorWithoutASessionIsIgnored() {
        repository.reportError("boom")
        settle()

        assertEquals(WalkingState.Idle, repository.status.value.state)
        assertNull(repository.status.value.errorMessage)
    }

    @Test
    fun routeStartsEmptyAndTracksOnlyAcceptedPoints() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        assertTrue(repository.route.value.isEmpty)

        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 30_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 30_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        currentTime = T0 + 31_000L
        location.emit(sample(latitude = 52.5, timestampMillis = T0 + 31_000L))
        settle()

        val route = repository.route.value
        assertEquals(listOf(52.0, 52.001), route.points.map { it.latitude })
        assertEquals(2, route.points.size)
    }

    @Test
    fun pauseBreaksTheRouteAndResumeKeepsTheRecordedHistory() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }

        repository.pauseWalk()
        awaitStatus(repository) { it.state == WalkingState.Paused }
        settle()

        assertEquals(2, repository.route.value.points.size)
        assertEquals(setOf(2), repository.route.value.breakBeforeIndexes)
        assertEquals(1, repository.route.value.segments().size)

        currentTime = T0 + 180_000L
        repository.resumeWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.002, timestampMillis = T0 + 180_000L))
        awaitRoute(repository) { it.points.size == 3 }
        settle()

        val segments = repository.route.value.segments()
        assertEquals(2, segments.size)
        assertEquals(listOf(52.0, 52.001), segments[0].map { it.latitude })
        assertEquals(listOf(52.002), segments[1].map { it.latitude })
    }

    @Test
    fun completedRouteSurvivesDismissOnlyUntilTheWalkEnds() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }

        repository.stopWalk()
        awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()
        assertEquals(2, repository.route.value.points.size)

        repository.dismissCompleted()
        awaitStatus(repository) { it.state == WalkingState.Idle }
        settle()
        assertTrue(repository.route.value.isEmpty)
    }

    @Test
    fun routeIsRestoredFromDatabaseAfterProcessDeath() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        settle()

        killScope()
        location.listener = null
        currentTime = T0 + 360_000L
        val recovered = createRepository()
        awaitStatus(recovered) { it.state == WalkingState.Active }
        settle()

        val route = recovered.route.value
        assertEquals(listOf(52.0, 52.001), route.points.map { it.latitude })
        assertEquals(listOf(0, 1), route.points.map { it.sequence })
    }

    @Test
    fun errorSessionRestoresItsRecordedRoute() {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        settle()

        repository.detachService()
        awaitStatus(repository) { it.state == WalkingState.Error }
        settle()

        killScope()
        location.listener = null
        val recovered = createRepository()
        awaitStatus(recovered) { it.state == WalkingState.Error }
        settle()

        assertEquals(2, recovered.route.value.points.size)
    }

    @Test
    fun startWalkCapturesTheSensorBaseline() {
        sensorCount = 5_000L

        repository.startWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        assertEquals(5_000L, status.walkStartStepCount)
    }

    @Test
    fun stopWalkSnapshotsStepsAsTheSensorDelta() {
        sensorCount = 5_000L
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        sensorCount = 7_800L
        repository.stopWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertEquals(2_800L, status.walkStepCount)
        val row = runBlocking { database.walkingDao().sessionById(status.sessionId!!) }
        assertEquals(5_000L, row?.walkStartStepCount)
        assertEquals(2_800L, row?.walkStepCount)
    }

    @Test
    fun stopWalkLeavesStepsUnavailableWithoutSensorCounts() {
        sensorCount = null

        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        repository.stopWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertNull(status.walkStartStepCount)
        assertNull(status.walkStepCount)
    }

    @Test
    fun stopWalkNeverReportsNegativeStepsAfterACounterReset() {
        sensorCount = 5_000L
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        sensorCount = 300L
        repository.stopWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertEquals(5_000L, status.walkStartStepCount)
        assertNull(status.walkStepCount)
    }

    @Test
    fun zeroSensorCountAtStartIsTreatedAsAnUnknownBaseline() {
        sensorCount = 0L

        repository.startWalk()
        val active = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        assertNull(active.walkStartStepCount)

        sensorCount = 100L
        repository.stopWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertNull(status.walkStepCount)
    }

    @Test
    fun lastCompletedSessionExposesTheFinishedWalk() {
        sensorCount = 1_000L
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        sensorCount = 1_650L
        repository.stopWalk()
        awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        val completed = repository.lastCompletedSession.value
        assertNotNull(completed)
        assertEquals(WalkingState.Completed, completed!!.state)
        assertEquals(1_000L, completed.walkStartStepCount)
        assertEquals(650L, completed.walkStepCount)
    }

    @Test
    fun lastCompletedSessionRestoredFromPersistedHistory() {
        val id = runBlocking {
            database.walkingDao().insertSession(
                WalkingSessionEntity(
                    state = WalkingState.Completed.name,
                    startMillis = 1_000L,
                    updatedMillis = 2_000L,
                    distanceMeters = 1_200.0,
                    activeMillis = 900_000L,
                    pausedMillis = 0L,
                    activeSegmentStartMillis = null,
                    pausedSegmentStartMillis = null,
                    startLatitude = 52.0,
                    startLongitude = 13.0,
                    lastLatitude = 52.01,
                    lastLongitude = 13.0,
                    errorMessage = null,
                    walkStartStepCount = 4_000L,
                    walkStepCount = 2_800L,
                ),
            )
        }

        val fresh = createRepository()
        val restored = runBlocking {
            withTimeout(15_000) { fresh.lastCompletedSession.first { it != null }!! }
        }

        assertEquals(id, restored.id)
        assertEquals(2_800L, restored.walkStepCount)
    }

    @Test
    fun recoveredWalkFinalizesStepsFromThePersistedBaseline() {
        sensorCount = 5_000L
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        killScope()
        sensorCount = 6_500L
        repository = createRepository()

        val recovered = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()
        assertEquals(5_000L, recovered.walkStartStepCount)

        repository.stopWalk()
        val status = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        assertEquals(1_500L, status.walkStepCount)
    }

    @Test
    fun completedHistoryExcludesActiveSessionsAndSortsNewestFirst() = runBlocking {
        val dao = database.walkingDao()
        val older = dao.insertSession(completedRow(startMillis = 100L))
        val newest = dao.insertSession(completedRow(startMillis = 500L))
        dao.insertSession(activeRow(startMillis = 900L))

        val history = repository.observeCompletedSessions().first()

        assertEquals(listOf(newest, older), history.map { it.id })
        assertTrue(history.all { it.state == WalkingState.Completed })
        assertEquals(listOf(500L, 100L), history.map { it.startMillis })
    }

    @Test
    fun routeForSessionLoadsPersistedPointsInRecordedOrder() = runBlocking {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        repository.stopWalk()
        val completed = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()

        val route = repository.routeForSession(completed.sessionId!!)

        assertEquals(listOf(0, 1), route.points.map { it.sequence })
        assertEquals(listOf(52.0, 52.001), route.points.map { it.latitude })
        assertTrue(repository.routeForSession(999L).isEmpty)
    }

    @Test
    fun routeForSessionFiltersPointsThatCannotBePlotted() = runBlocking {
        val dao = database.walkingDao()
        val id = dao.insertSession(completedRow(startMillis = 300L))
        dao.insertLocationPoint(row(id, sequence = 0, latitude = 52.0))
        dao.insertLocationPoint(row(id, sequence = 1, latitude = 999.0))
        dao.insertLocationPoint(row(id, sequence = 2, longitude = 999.0))
        dao.insertLocationPoint(row(id, sequence = 3, latitude = 52.002))

        val route = repository.routeForSession(id)

        assertEquals(listOf(0, 3), route.points.map { it.sequence })
        assertEquals(listOf(52.0, 52.002), route.points.map { it.latitude })
    }

    @Test
    fun deleteActivityRemovesTheWalkAndClearsTheLastCompletedSession() = runBlocking {
        repository.startWalk()
        awaitStatus(repository) { it.state == WalkingState.Active }
        location.emit(sample(latitude = 52.0, timestampMillis = T0))
        awaitStatus(repository) { it.hasFix }
        currentTime = T0 + 60_000L
        location.emit(sample(latitude = 52.001, timestampMillis = T0 + 60_000L))
        awaitStatus(repository) { it.distanceMeters > 50.0 }
        repository.stopWalk()
        val completed = awaitStatus(repository) { it.state == WalkingState.Completed }
        settle()
        val id = completed.sessionId!!
        assertNotNull(repository.lastCompletedSession.value)

        val removed = repository.deleteActivity(id)
        settle()

        assertTrue(removed)
        assertNull(repository.lastCompletedSession.value)
        assertEquals(0, database.walkingDao().sessionCount())
        assertTrue(database.walkingDao().locationPoints(id).isEmpty())
    }

    @Test
    fun deleteActivityRefusesAnOngoingSession() = runBlocking {
        repository.startWalk()
        val active = awaitStatus(repository) { it.state == WalkingState.Active }
        settle()

        val removed = repository.deleteActivity(active.sessionId!!)
        settle()

        assertFalse(removed)
        assertEquals(1, database.walkingDao().sessionCount())
        assertEquals(WalkingState.Active, repository.status.value.state)
    }

    private fun completedRow(startMillis: Long) = WalkingSessionEntity(
        state = WalkingState.Completed.name,
        startMillis = startMillis,
        updatedMillis = startMillis + 60_000L,
        distanceMeters = 1_000.0,
        activeMillis = 600_000L,
        pausedMillis = 0L,
        activeSegmentStartMillis = null,
        pausedSegmentStartMillis = null,
        startLatitude = 52.0,
        startLongitude = 13.0,
        lastLatitude = 52.01,
        lastLongitude = 13.01,
        errorMessage = null,
    )

    private fun activeRow(startMillis: Long) = WalkingSessionEntity(
        state = WalkingState.Active.name,
        startMillis = startMillis,
        updatedMillis = startMillis,
        distanceMeters = 0.0,
        activeMillis = 0L,
        pausedMillis = 0L,
        activeSegmentStartMillis = startMillis,
        pausedSegmentStartMillis = null,
        startLatitude = null,
        startLongitude = null,
        lastLatitude = null,
        lastLongitude = null,
        errorMessage = null,
    )

    private fun row(
        sessionId: Long,
        sequence: Int,
        latitude: Double = 52.0,
        longitude: Double = 13.0,
    ) = WalkingLocationPointEntity(
        sessionId = sessionId,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = T0 + sequence * 1_000L,
        accuracyMeters = 5f,
        altitudeMeters = null,
        speedMps = null,
    )

    private fun createRepository(): WalkingRepositoryImpl = WalkingRepositoryImpl(
        locationDataSource = location,
        dao = database.walkingDao(),
        permissionState = { permission },
        now = { currentTime },
        measureDistance = { from, to -> flatDistance(from, to) },
        stepCountNow = { sensorCount },
        scope = testScope,
    )

    private fun killScope() {
        scopes.remove(testScope)
        testScope.cancel()
        testScope = CoroutineScope(Dispatchers.Unconfined)
        scopes.add(testScope)
    }

    private fun awaitStatus(
        target: WalkingRepositoryImpl,
        predicate: (WalkingStatus) -> Boolean,
    ): WalkingStatus = runBlocking {
        withTimeout(15_000) { target.status.first(predicate) }
    }

    private fun awaitRoute(
        target: WalkingRepositoryImpl,
        predicate: (WalkRoute) -> Boolean,
    ): WalkRoute = runBlocking {
        withTimeout(15_000) { target.route.first(predicate) }
    }

    private fun settle() = runBlocking {
        withTimeout(15_000) {
            testScope.coroutineContext.job.children.toList().joinAll()
        }
    }

    /**
     * StateFlow conflates while slow collectors are queued, so drain the Unconfined event
     * loop between asserting a state and triggering the next transition. This keeps the
     * history assertion deterministic instead of racing the collector.
     */
    private fun allowCollectorToCatchUp() = runBlocking {
        withTimeout(15_000) { delay(25) }
    }

    private fun CoroutineScope.launchCollector(
        target: WalkingRepositoryImpl,
        onState: (WalkingState) -> Unit,
    ) = launch {
        target.status.collect { onState(it.state) }
    }

    private fun sample(
        latitude: Double,
        timestampMillis: Long,
        longitude: Double = 13.0,
        accuracyMeters: Float? = 5f,
    ) = LocationSample(latitude, longitude, timestampMillis, accuracyMeters)

    private fun flatDistance(from: LocationSample, to: LocationSample): Double {
        val meanLatitude = Math.toRadians((from.latitude + to.latitude) / 2.0)
        val east = Math.toRadians(to.longitude - from.longitude) * cos(meanLatitude) * EARTH_RADIUS_METERS
        val north = Math.toRadians(to.latitude - from.latitude) * EARTH_RADIUS_METERS
        return hypot(east, north)
    }

    private companion object {
        const val T0 = 1_700_000_000_000L
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
