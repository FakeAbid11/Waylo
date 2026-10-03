package com.waylo.app.data.achievement

import androidx.room.Room
import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.core.common.AchievementEvaluator
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.local.WalkingSessionEntity
import com.waylo.app.data.local.XpAwardEntity
import com.waylo.app.data.progression.recalculateProgression
import com.waylo.app.domain.model.AchievementRequirement
import com.waylo.app.domain.model.WalkingState
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
class AchievementRepositoryTest {

    private lateinit var database: WayloDatabase
    private lateinit var repository: AchievementRepositoryImpl
    private var currentTime = dayOne

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()
        currentTime = dayOne
        repository = createRepository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun freshInstallUnlocksNothingAndEmitsNoEvent() = runBlocking {
        val unlocked = repository.reconcile()

        assertTrue(unlocked.isEmpty())
        assertNull(repository.unlockEvent.value)
        assertEquals(0, database.achievementDao().unlockCount())
    }

    @Test
    fun aCompletedWalkUnlocksFirstWalkOnce() = runBlocking {
        val activityId = insertCompletedWalk(distanceMeters = 500.0)

        val unlocked = repository.reconcile(activityId)

        assertEquals(listOf("first_walk"), unlocked)
        val stored = database.achievementDao().unlockFor("first_walk")
        assertNotNull(stored)
        assertEquals(currentTime, stored!!.unlockedAtMillis)

        val event = repository.unlockEvent.value
        assertNotNull(event)
        assertEquals(activityId, event!!.activityId)
        assertEquals(listOf("first_walk"), event.unlocks.map { it.id })
        assertEquals(currentTime, event.unlockedAtMillis)
    }

    @Test
    fun reconciliationIsIdempotentAcrossRepeatedCalls() = runBlocking {
        insertCompletedWalk(distanceMeters = 1_200.0, walkSteps = 1_500L)

        val first = repository.reconcile()
        repository.consumeUnlockEvent()
        val second = repository.reconcile()

        assertEquals(listOf("first_walk", "distance_1km", "steps_1000"), first)
        assertTrue(second.isEmpty())
        assertEquals(3, database.achievementDao().unlockCount())
        assertNull(repository.unlockEvent.value)
    }

    @Test
    fun oneWalkCanUnlockSeveralAchievementsInASingleEvent() = runBlocking {
        val activityId = insertCompletedWalk(distanceMeters = 1_200.0, walkSteps = 1_500L)

        val unlocked = repository.reconcile(activityId)

        assertEquals(listOf("first_walk", "distance_1km", "steps_1000"), unlocked)
        val event = repository.unlockEvent.value
        assertNotNull(event)
        assertEquals(3, event!!.unlocks.size)
        assertEquals(3, database.achievementDao().unlockCount())
    }

    @Test
    fun existingHistoryRetroactivelyUnlocksMilestonesFromPersistedData() = runBlocking {
        repeat(12) {
            insertCompletedWalk(distanceMeters = 1_000.0, walkSteps = 1_000L)
        }

        val unlocked = repository.reconcile().toSet()

        assertTrue("first_walk" in unlocked)
        assertTrue("walks_10" in unlocked)
        assertFalse("walks_50" in unlocked)
        assertTrue("distance_1km" in unlocked)
        assertTrue("distance_5km" in unlocked)
        assertTrue("distance_10km" in unlocked)
        assertFalse("distance_50km" in unlocked)
        assertTrue("steps_1000" in unlocked)
        assertTrue("steps_10000" in unlocked)
        assertFalse("steps_50000" in unlocked)
        assertEquals(7, unlocked.size)
    }

    @Test
    fun xpLevelAndStreakAchievementsComeFromThePhaseEightLedger() = runBlocking {
        val dayOneId = insertCompletedWalk(distanceMeters = 1_500.0, atMillis = dayOne)
        awardXp(dayOneId, xp = 400, atMillis = dayOne)

        currentTime = dayTwo
        val dayTwoId = insertCompletedWalk(distanceMeters = 1_500.0, atMillis = dayTwo)
        awardXp(dayTwoId, xp = 400, atMillis = dayTwo)

        currentTime = dayThree
        val dayThreeId = insertCompletedWalk(distanceMeters = 1_500.0, atMillis = dayThree)
        awardXp(dayThreeId, xp = 400, atMillis = dayThree)

        val unlocked = repository.reconcile().toSet()

        assertTrue("xp_1000" in unlocked)
        assertFalse("xp_5000" in unlocked)
        assertTrue("streak_3" in unlocked)
        assertFalse("streak_7" in unlocked)
        // 1,200 XP is level 3 — level 5 needs 3,000 XP.
        assertFalse("level_5" in unlocked)
        assertEquals(3, repository.snapshot.first().context.level)
        assertEquals(3, repository.snapshot.first().context.longestStreakDays)
    }

    @Test
    fun deletingAWalkNeverRevokesAnUnlockedAchievement() = runBlocking {
        val activityId = insertCompletedWalk(distanceMeters = 1_200.0)
        repository.reconcile(activityId)
        assertNotNull(database.achievementDao().unlockFor("distance_1km"))

        val removed = database.recalculateProgressionAndDelete(activityId)

        assertTrue(removed)
        // Unlock rows survive deletion — milestones already reached are permanent.
        assertEquals(2, database.achievementDao().unlockCount())

        val snapshot = repository.snapshot.first()
        assertTrue("distance_1km" in snapshot.unlockedIds)
        assertTrue("first_walk" in snapshot.unlockedIds)
        assertEquals(0L, snapshot.context.totalDistanceMeters)

        assertTrue(repository.reconcile().isEmpty())
        assertEquals(2, database.achievementDao().unlockCount())
    }

    @Test
    fun unlockEventIsAOneShotThatCanBeConsumedAndNeverReplayed() = runBlocking {
        insertCompletedWalk(distanceMeters = 500.0)
        repository.reconcile()
        assertNotNull(repository.unlockEvent.value)

        repository.consumeUnlockEvent()
        assertNull(repository.unlockEvent.value)

        repository.reconcile()
        assertNull(repository.unlockEvent.value)
    }

    @Test
    fun snapshotProgressTracksPersistedTotalsWithoutStoredSnapshots() = runBlocking {
        val initial = repository.snapshot.first()
        assertEquals(0L, initial.context.totalDistanceMeters)
        assertEquals(0, initial.context.completedWalkCount)
        assertTrue(initial.unlockedIds.isEmpty())

        insertCompletedWalk(distanceMeters = 4_000.0)
        val progressed = repository.snapshot.first()
        assertEquals(4_000L, progressed.context.totalDistanceMeters)
        assertEquals(1, progressed.context.completedWalkCount)

        val requirement = AchievementRequirement.DistanceMeters(meters = 5_000L)
        assertEquals(
            4_000L,
            AchievementEvaluator.currentValue(requirement, progressed.context),
        )
        assertFalse("distance_5km" in progressed.unlockedIds)
        assertTrue("distance_1km" in progressed.unlockedIds)
    }

    @Test
    fun snapshotUnionsPersistedUnlocksWithCurrentlySatisfiedIds() = runBlocking {
        val activityId = insertCompletedWalk(distanceMeters = 1_200.0)
        repository.reconcile(activityId)
        repository.consumeUnlockEvent()

        // Persisted unlock survives even when the source data can no longer
        // satisfy the requirement (totals drop back to zero after deletion).
        database.recalculateProgressionAndDelete(activityId)
        val snapshot = repository.snapshot.first()

        assertTrue("distance_1km" in snapshot.unlockedIds)
        assertEquals(0L, snapshot.context.totalDistanceMeters)
        assertNotNull(snapshot.unlockedAtById["distance_1km"])
    }

    @Test
    fun summaryReportsUnlockedOverTotalCounts() = runBlocking {
        val before = repository.summary.first()
        assertEquals(0, before.unlockedCount)
        assertEquals(AchievementCatalog.definitions.size, before.totalCount)

        insertCompletedWalk(distanceMeters = 500.0)
        repository.reconcile()

        val after = repository.summary.first()
        assertEquals(1, after.unlockedCount)
        assertEquals(AchievementCatalog.definitions.size, after.totalCount)
    }

    @Test
    fun aRestartedRepositoryReconcilesLostWorkExactlyOnce() = runBlocking {
        // Walk completed, process died before any evaluation ran.
        insertCompletedWalk(distanceMeters = 1_200.0, walkSteps = 1_500L)

        val restarted = createRepository()
        val unlocked = restarted.reconcile()
        assertEquals(listOf("first_walk", "distance_1km", "steps_1000"), unlocked)

        val secondStart = createRepository()
        assertTrue(secondStart.reconcile().isEmpty())
        assertNull(secondStart.unlockEvent.value)
        assertEquals(3, database.achievementDao().unlockCount())
    }

    @Test
    fun walkStepTotalsIgnoreWalksWithoutSensorData() = runBlocking {
        insertCompletedWalk(distanceMeters = 900.0, walkSteps = null)

        val snapshot = repository.snapshot.first()

        // Steps are zero (no sensor data), while every other aggregate still
        // reflects the persisted walk — nothing is fabricated.
        assertEquals(0L, snapshot.context.totalSteps)
        assertEquals(900L, snapshot.context.totalDistanceMeters)
        assertEquals(600L, snapshot.context.totalActiveSeconds)
    }

    private suspend fun WayloDatabase.recalculateProgressionAndDelete(activityId: Long): Boolean =
        com.waylo.app.data.progression.ProgressionRepositoryImpl(
            database = database,
            now = { currentTime },
            zone = ZoneOffset.UTC,
        ).deleteActivityCascade(activityId) {
            walkingDao().deleteActivity(activityId)
        }

    private suspend fun insertCompletedWalk(
        distanceMeters: Double,
        walkSteps: Long? = null,
        atMillis: Long = currentTime,
    ): Long = database.walkingDao().insertSession(
        WalkingSessionEntity(
            state = WalkingState.Completed.name,
            startMillis = atMillis,
            updatedMillis = atMillis + 60_000L,
            distanceMeters = distanceMeters,
            activeMillis = 600_000L,
            pausedMillis = 0L,
            activeSegmentStartMillis = null,
            pausedSegmentStartMillis = null,
            startLatitude = 52.0,
            startLongitude = 13.0,
            lastLatitude = 52.01,
            lastLongitude = 13.01,
            errorMessage = null,
            walkStartStepCount = null,
            walkStepCount = walkSteps,
        ),
    )

    private suspend fun awardXp(activityId: Long, xp: Int, atMillis: Long) {
        database.progressionDao().insertAward(
            XpAwardEntity(activityId = activityId, xp = xp, awardedAt = atMillis),
        )
        database.recalculateProgression(atMillis, ZoneOffset.UTC)
    }

    private fun createRepository() = AchievementRepositoryImpl(
        database = database,
        now = { currentTime },
        zone = ZoneOffset.UTC,
    )

    private companion object {
        private val dayOne = epoch(LocalDateTime.of(2026, 10, 1, 10, 0))
        private val dayTwo = epoch(LocalDateTime.of(2026, 10, 2, 10, 0))
        private val dayThree = epoch(LocalDateTime.of(2026, 10, 3, 10, 0))

        private fun epoch(localDateTime: LocalDateTime): Long =
            localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
