package com.waylo.app.data.progression

import androidx.room.Room
import com.waylo.app.data.local.ProgressionEntity
import com.waylo.app.data.local.WayloDatabase
import com.waylo.app.data.local.WalkingSessionEntity
import com.waylo.app.domain.model.AwardOutcome
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
class ProgressionRepositoryTest {

    private lateinit var database: WayloDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private var currentTime = dayOne

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()
        currentTime = dayOne
        progression = createProgression()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun qualifyingWalkAwardsXpAndKeepsLedgerAndRowInSync() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)

        val result = progression.awardActivityXp(activityId)

        assertEquals(AwardOutcome.Awarded, result.outcome)
        assertEquals(150, result.xpAwarded)
        assertEquals(150, result.totalXp)
        assertEquals(1, result.levelBefore)
        assertEquals(2, result.levelAfter)
        assertTrue(result.leveledUp)
        assertEquals(1, result.currentStreakDays)
        assertEquals(1, result.longestStreakDays)

        val award = database.progressionDao().awardForActivity(activityId)
        assertNotNull(award)
        assertEquals(150, award!!.xp)
        assertEquals(currentTime, award.awardedAt)

        val total = database.progressionDao().totalXp()
        val row = database.progressionDao().progressionById(ProgressionEntity.SINGLE_ROW_ID)
        assertNotNull(row)
        assertEquals(total, row!!.totalXp)
        assertEquals(150, row.totalXp)
        assertEquals(1, row.currentStreakDays)
        assertEquals(1, row.longestStreakDays)

        val event = progression.awardEvent.value
        assertNotNull(event)
        assertEquals(activityId, event!!.activityId)
        assertEquals(150, event.xpAwarded)
        assertEquals(1, event.levelBefore)
        assertEquals(2, event.levelAfter)
    }

    @Test
    fun awardIsIdempotentAcrossRetriesAndRepositoryRestarts() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)

        val first = progression.awardActivityXp(activityId)
        val second = progression.awardActivityXp(activityId)

        assertEquals(AwardOutcome.Awarded, first.outcome)
        assertEquals(AwardOutcome.AlreadyAwarded, second.outcome)
        assertEquals(150, second.xpAwarded)
        assertEquals(150, second.totalXp)
        assertEquals(2, second.levelAfter)
        assertEquals(1, database.progressionDao().awardCount())
        assertEquals(150, database.progressionDao().totalXp())

        val restarted = createProgression()
        val afterRestart = restarted.awardActivityXp(activityId)

        assertEquals(AwardOutcome.AlreadyAwarded, afterRestart.outcome)
        assertEquals(150, afterRestart.totalXp)
        assertEquals(1, database.progressionDao().awardCount())
        assertNull(restarted.awardEvent.value)
        assertEquals(
            150,
            database.progressionDao()
                .progressionById(ProgressionEntity.SINGLE_ROW_ID)!!
                .totalXp,
        )
    }

    @Test
    fun shortWalkEarnsNoXpAndNeverTouchesTheLedger() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 50.0)

        val result = progression.awardActivityXp(activityId)

        assertEquals(AwardOutcome.NotQualifying, result.outcome)
        assertEquals(0, result.xpAwarded)
        assertEquals(0, result.totalXp)
        assertEquals(1, result.levelBefore)
        assertEquals(1, result.levelAfter)
        assertEquals(0, database.progressionDao().awardCount())
        assertNull(database.progressionDao().progressionById(ProgressionEntity.SINGLE_ROW_ID))
        assertNull(progression.awardEvent.value)
    }

    @Test
    fun awardRefusesSessionsThatAreNotPersistedAsCompleted() = runBlocking {
        val activeId = database.walkingDao().insertSession(
            session(state = WalkingState.Active.name, distanceMeters = 1_500.0),
        )

        val missing = progression.awardActivityXp(404L)
        val active = progression.awardActivityXp(activeId)

        assertEquals(AwardOutcome.NotPersisted, missing.outcome)
        assertEquals(AwardOutcome.NotPersisted, active.outcome)
        assertEquals(0, database.progressionDao().awardCount())
        assertNull(progression.awardEvent.value)
    }

    @Test
    fun streaksFollowConsecutiveQualifyingLocalDays() = runBlocking {
        val dayOneId = insertCompletedSession(distanceMeters = 1_500.0)
        val dayTwoId = insertCompletedSession(distanceMeters = 1_500.0)

        progression.awardActivityXp(dayOneId)

        currentTime = dayTwo
        val dayTwoResult = progression.awardActivityXp(dayTwoId)
        assertEquals(2, dayTwoResult.currentStreakDays)
        assertEquals(2, dayTwoResult.longestStreakDays)

        currentTime = dayFive
        val dayFiveId = insertCompletedSession(distanceMeters = 1_500.0)
        val dayFiveResult = progression.awardActivityXp(dayFiveId)
        assertEquals(1, dayFiveResult.currentStreakDays)
        assertEquals(2, dayFiveResult.longestStreakDays)

        val row = database.progressionDao().progressionById(ProgressionEntity.SINGLE_ROW_ID)
        assertEquals(1, row!!.currentStreakDays)
        assertEquals(2, row.longestStreakDays)
        assertEquals(450, row.totalXp)
    }

    @Test
    fun deletingAnActivityRemovesItsAwardAndRecalculatesProgression() = runBlocking {
        val firstId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(firstId)
        currentTime = dayTwo
        val secondId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(secondId)
        assertEquals(300, database.progressionDao().totalXp())

        val removed = progression.deleteActivityCascade(secondId) {
            database.walkingDao().deleteActivity(secondId)
        }

        assertTrue(removed)
        assertEquals(1, database.progressionDao().awardCount())
        assertNull(database.progressionDao().awardForActivity(secondId))
        assertNotNull(database.progressionDao().awardForActivity(firstId))
        assertEquals(150, database.progressionDao().totalXp())
        val row = database.progressionDao().progressionById(ProgressionEntity.SINGLE_ROW_ID)
        assertEquals(150, row!!.totalXp)
        assertEquals(1, row.currentStreakDays)
        assertEquals(1, row.longestStreakDays)
        assertEquals(1, database.walkingDao().sessionCount())
    }

    @Test
    fun deletingAnActivityThatNeverEarnedXpStillSucceeds() = runBlocking {
        val awardedId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(awardedId)
        val unawardedId = insertCompletedSession(distanceMeters = 1_500.0)

        val removed = progression.deleteActivityCascade(unawardedId) {
            database.walkingDao().deleteActivity(unawardedId)
        }

        assertTrue(removed)
        assertEquals(1, database.progressionDao().awardCount())
        assertEquals(150, database.progressionDao().totalXp())
        val row = database.progressionDao().progressionById(ProgressionEntity.SINGLE_ROW_ID)
        assertEquals(150, row!!.totalXp)
        assertEquals(1, row.currentStreakDays)
    }

    @Test
    fun deletingAnUnknownActivityMutatesNothing() = runBlocking {
        val awardedId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(awardedId)

        val removed = progression.deleteActivityCascade(999L) {
            database.walkingDao().deleteActivity(999L)
        }

        assertFalse(removed)
        assertEquals(150, database.progressionDao().totalXp())
        assertEquals(1, database.walkingDao().sessionCount())
    }

    @Test
    fun progressFlowExposesLevelsStreaksAndLifetimeTotals() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(activityId)

        val progress = progression.progress.first()

        assertEquals(2, progress.level)
        assertEquals(150, progress.totalXp)
        assertEquals(50, progress.xp)
        assertEquals(400, progress.xpToNextLevel)
        assertEquals(1, progress.currentStreakDays)
        assertEquals(1, progress.longestStreakDays)
        assertEquals(1.5, progress.totalDistanceKm, 0.001)
        assertEquals(10, progress.totalWalkingMinutes)
        assertEquals(2_000, progress.totalSteps)
    }

    @Test
    fun recoveryAwardsWalksLostAfterTheEraButSkipsPreEraHistory() = runBlocking {
        val eraAnchorId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(eraAnchorId)
        val eraStart = database.progressionDao()
            .progressionById(ProgressionEntity.SINGLE_ROW_ID)!!
            .createdAtMillis

        val historicalId = insertCompletedSession(
            distanceMeters = 1_500.0,
            startMillis = eraStart - 100_000L,
            updatedMillis = eraStart - 50_000L,
        )
        progression.recoverAwardIfNeeded(historicalId, eraStart - 50_000L)
        assertNull(database.progressionDao().awardForActivity(historicalId))

        val lostId = insertCompletedSession(
            distanceMeters = 1_500.0,
            startMillis = eraStart + 1_000L,
            updatedMillis = eraStart + 2_000L,
        )
        progression.recoverAwardIfNeeded(lostId, eraStart + 2_000L)
        assertNotNull(database.progressionDao().awardForActivity(lostId))
        assertEquals(300, database.progressionDao().totalXp())
    }

    @Test
    fun recoveryRepeatingAfterAnAwardIsHarmless() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(activityId)

        progression.recoverAwardIfNeeded(activityId, currentTime)
        progression.recoverAwardIfNeeded(activityId, currentTime)

        assertEquals(1, database.progressionDao().awardCount())
        assertEquals(150, database.progressionDao().totalXp())
        assertNotNull(progression.awardEvent.value)
    }

    @Test
    fun awardEventIsAOneShotThatCanBeConsumed() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(activityId)

        assertNotNull(progression.awardEvent.value)

        progression.consumeAwardEvent()

        assertNull(progression.awardEvent.value)
    }

    @Test
    fun oneShotEventIsNotReplayedAfterARestart() = runBlocking {
        val activityId = insertCompletedSession(distanceMeters = 1_500.0)
        progression.awardActivityXp(activityId)
        progression.consumeAwardEvent()

        val restarted = createProgression()
        restarted.awardActivityXp(activityId)

        assertNull(restarted.awardEvent.value)
    }

    private suspend fun insertCompletedSession(
        distanceMeters: Double,
        startMillis: Long = currentTime,
        updatedMillis: Long = currentTime + 60_000L,
    ): Long = database.walkingDao().insertSession(
        session(
            state = WalkingState.Completed.name,
            distanceMeters = distanceMeters,
            startMillis = startMillis,
            updatedMillis = updatedMillis,
        ),
    )

    private fun session(
        state: String,
        distanceMeters: Double,
        startMillis: Long = currentTime,
        updatedMillis: Long = currentTime + 60_000L,
    ) = WalkingSessionEntity(
        state = state,
        startMillis = startMillis,
        updatedMillis = updatedMillis,
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
        walkStartStepCount = 8_000L,
        walkStepCount = 2_000L,
    )

    private fun createProgression() = ProgressionRepositoryImpl(
        database = database,
        now = { currentTime },
        zone = ZoneOffset.UTC,
    )

    companion object {
        private val dayOne = epoch(LocalDateTime.of(2026, 10, 1, 10, 0))
        private val dayTwo = epoch(LocalDateTime.of(2026, 10, 2, 10, 0))
        private val dayFive = epoch(LocalDateTime.of(2026, 10, 5, 10, 0))

        private fun epoch(localDateTime: LocalDateTime): Long =
            localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
