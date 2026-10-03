package com.waylo.app.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProgressionDaoTest {

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
    fun duplicateAwardsForTheSameActivityAreIgnored() = runBlocking {
        val dao = database.progressionDao()

        val first = dao.insertAward(XpAwardEntity(activityId = 7L, xp = 150, awardedAt = 1_000L))
        val second = dao.insertAward(XpAwardEntity(activityId = 7L, xp = 999, awardedAt = 2_000L))

        assertTrue(first > 0L)
        assertEquals(-1L, second)
        assertEquals(1, dao.awardCount())
        assertEquals(150, dao.totalXp())
        assertEquals(150, dao.awardForActivity(7L)?.xp)
    }

    @Test
    fun totalXpSumsTheWholeLedger() = runBlocking {
        val dao = database.progressionDao()
        dao.insertAward(XpAwardEntity(activityId = 1L, xp = 150, awardedAt = 1_000L))
        dao.insertAward(XpAwardEntity(activityId = 2L, xp = 40, awardedAt = 2_000L))

        assertEquals(190, dao.totalXp())
        assertEquals(2, dao.awardCount())
    }

    @Test
    fun deletingAnAwardOnlyRemovesThatActivity() = runBlocking {
        val dao = database.progressionDao()
        dao.insertAward(XpAwardEntity(activityId = 1L, xp = 150, awardedAt = 1_000L))
        dao.insertAward(XpAwardEntity(activityId = 2L, xp = 40, awardedAt = 2_000L))

        val removed = dao.deleteAwardForActivity(1L)

        assertEquals(1, removed)
        assertNull(dao.awardForActivity(1L))
        assertNotNull(dao.awardForActivity(2L))
        assertEquals(40, dao.totalXp())
    }

    @Test
    fun awardXpFlowReflectsThePersistedRow() = runBlocking {
        val dao = database.progressionDao()

        dao.insertAward(XpAwardEntity(activityId = 7L, xp = 150, awardedAt = 1_000L))
        assertEquals(150, dao.observeAwardXp(7L).first())

        dao.deleteAwardForActivity(7L)
        assertNull(dao.observeAwardXp(7L).first())
        assertNull(dao.observeAwardXp(9L).first())
    }

    @Test
    fun qualifyingAwardDatesExcludeZeroXpRows() = runBlocking {
        val dao = database.progressionDao()
        dao.insertAward(XpAwardEntity(activityId = 1L, xp = 150, awardedAt = 1_000L))
        dao.insertAward(XpAwardEntity(activityId = 2L, xp = 0, awardedAt = 2_000L))

        assertEquals(listOf(1_000L), dao.qualifyingAwardMillis())
        assertEquals(listOf(1_000L), dao.observeQualifyingAwardMillis().first())
    }

    @Test
    fun completedTotalsSumOnlyCompletedSessions() = runBlocking {
        val walking = database.walkingDao()
        walking.insertSession(
            session(
                state = "Completed",
                startMillis = 100L,
                distanceMeters = 1_500.0,
                activeMillis = 600_000L,
                walkStepCount = 2_000L,
            ),
        )
        walking.insertSession(
            session(
                state = "Completed",
                startMillis = 200L,
                distanceMeters = 500.0,
                activeMillis = 120_000L,
                walkStepCount = 700L,
            ),
        )
        walking.insertSession(
            session(state = "Active", startMillis = 300L, distanceMeters = 9_999.0),
        )

        val totals = database.progressionDao().completedTotals()

        assertEquals(2_700L, totals.steps)
        assertEquals(2_000.0, totals.distanceMeters, 0.001)
        assertEquals(720_000L, totals.activeMillis)

        val observed = database.progressionDao().observeCompletedTotals().first()
        assertEquals(totals, observed)
    }

    @Test
    fun progressionRowRoundTripsThroughUpsert() = runBlocking {
        val dao = database.progressionDao()

        assertNull(dao.progressionById(ProgressionEntity.SINGLE_ROW_ID))

        dao.upsertProgression(
            ProgressionEntity(
                id = ProgressionEntity.SINGLE_ROW_ID,
                totalXp = 150,
                currentStreakDays = 1,
                longestStreakDays = 3,
                updatedAtMillis = 5_000L,
                createdAtMillis = 1_000L,
            ),
        )
        dao.upsertProgression(
            ProgressionEntity(
                id = ProgressionEntity.SINGLE_ROW_ID,
                totalXp = 300,
                currentStreakDays = 2,
                longestStreakDays = 3,
                updatedAtMillis = 9_000L,
                createdAtMillis = 1_000L,
            ),
        )

        val stored = dao.progressionById(ProgressionEntity.SINGLE_ROW_ID)
        assertNotNull(stored)
        assertEquals(300, stored!!.totalXp)
        assertEquals(2, stored.currentStreakDays)
        assertEquals(3, stored.longestStreakDays)
        assertEquals(1_000L, stored.createdAtMillis)
        assertEquals(
            stored,
            dao.observeProgressionById(ProgressionEntity.SINGLE_ROW_ID).first(),
        )
    }

    private fun session(
        state: String,
        startMillis: Long,
        distanceMeters: Double = 0.0,
        activeMillis: Long = 0L,
        walkStepCount: Long? = null,
    ) = WalkingSessionEntity(
        state = state,
        startMillis = startMillis,
        updatedMillis = startMillis + 60_000L,
        distanceMeters = distanceMeters,
        activeMillis = activeMillis,
        pausedMillis = 0L,
        activeSegmentStartMillis = null,
        pausedSegmentStartMillis = null,
        startLatitude = 52.0,
        startLongitude = 13.0,
        lastLatitude = 52.01,
        lastLongitude = 13.01,
        errorMessage = null,
        walkStartStepCount = walkStepCount?.let { it - 100L },
        walkStepCount = walkStepCount,
    )
}
