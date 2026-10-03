package com.waylo.app.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WalkingDaoTest {

    private lateinit var database: WayloDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completedSessionsAreSortedNewestFirstAndExcludeActiveSessions() = runBlocking {
        val dao = database.walkingDao()
        dao.insertSession(session(state = "Active", startMillis = 900L))
        dao.insertSession(session(state = "Completed", startMillis = 100L))
        dao.insertSession(session(state = "Completed", startMillis = 500L))

        val completed = dao.observeCompletedSessions().first()

        assertEquals(listOf(500L, 100L), completed.map { it.startMillis })
        assertTrue(completed.all { it.state == "Completed" })
    }

    @Test
    fun completedSessionsBreakStartTiesWithTheNewerRowFirst() = runBlocking {
        val dao = database.walkingDao()
        val older = dao.insertSession(session(state = "Completed", startMillis = 300L))
        val newer = dao.insertSession(session(state = "Completed", startMillis = 300L))

        val completed = dao.observeCompletedSessions().first()

        assertEquals(listOf(newer, older), completed.map { it.id })
    }

    @Test
    fun observeSessionReturnsTheMatchingRowOrNothing() = runBlocking {
        val dao = database.walkingDao()
        val id = dao.insertSession(session(state = "Completed", startMillis = 300L))
        dao.insertSession(session(state = "Completed", startMillis = 400L))

        val session = dao.observeSession(id).first()

        assertEquals(id, session?.id)
        assertNull(dao.observeSession(999L).first())
    }

    @Test
    fun completedSessionsFlowReactsToInsertionsAndDeletions() = runBlocking {
        val dao = database.walkingDao()
        val firstRow = launch {
            withTimeout(15_000) { dao.observeCompletedSessions().first { it.size == 1 } }
        }
        val id = dao.insertSession(session(state = "Completed", startMillis = 200L))
        firstRow.join()

        val removal = launch {
            withTimeout(15_000) { dao.observeCompletedSessions().first { it.isEmpty() } }
        }
        assertTrue(dao.deleteActivity(id))
        removal.join()

        assertEquals(0, dao.sessionCount())
    }

    @Test
    fun deleteActivityRemovesTheSessionAndItsRoutePoints() = runBlocking {
        val dao = database.walkingDao()
        val target = dao.insertSession(session(state = "Completed", startMillis = 300L))
        val survivor = dao.insertSession(session(state = "Completed", startMillis = 400L))
        dao.insertLocationPoint(point(sessionId = target, sequence = 0))
        dao.insertLocationPoint(point(sessionId = target, sequence = 1))
        dao.insertLocationPoint(point(sessionId = survivor, sequence = 0))

        val removed = dao.deleteActivity(target)

        assertTrue(removed)
        assertNull(dao.sessionById(target))
        assertTrue(dao.locationPoints(target).isEmpty())
        assertEquals(1, dao.sessionCount())
        assertEquals(1, dao.locationPoints(survivor).size)
    }

    @Test
    fun deleteActivityRefusesActiveSessionsAndKeepsTheirPoints() = runBlocking {
        val dao = database.walkingDao()
        val active = dao.insertSession(session(state = "Active", startMillis = 300L))
        dao.insertLocationPoint(point(sessionId = active, sequence = 0))

        val removed = dao.deleteActivity(active)

        assertFalse(removed)
        assertEquals("Active", dao.sessionById(active)?.state)
        assertEquals(1, dao.locationPoints(active).size)
    }

    @Test
    fun deleteActivityReturnsFalseForAnUnknownId() = runBlocking {
        val dao = database.walkingDao()

        assertFalse(dao.deleteActivity(42L))
    }

    private fun session(state: String, startMillis: Long) = WalkingSessionEntity(
        state = state,
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

    private fun point(sessionId: Long, sequence: Int) = WalkingLocationPointEntity(
        sessionId = sessionId,
        sequence = sequence,
        latitude = 52.0 + sequence * 0.001,
        longitude = 13.0,
        timestampMillis = 1_000L + sequence,
        accuracyMeters = 5f,
        altitudeMeters = null,
        speedMps = null,
    )
}
