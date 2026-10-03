package com.waylo.app.ui.history

import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.core.common.WorkoutStatisticsCalculator
import com.waylo.app.data.achievement.AchievementRepository
import com.waylo.app.data.map.MapCameraCommand
import com.waylo.app.data.map.WalkMapCameraPolicy
import com.waylo.app.data.map.WalkMapLoadState
import com.waylo.app.data.progression.ProgressionRepository
import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementSnapshot
import com.waylo.app.domain.model.AchievementSummary
import com.waylo.app.domain.model.AchievementUnlock
import com.waylo.app.domain.model.AchievementUnlockedEvent
import com.waylo.app.domain.model.ProgressionAwardEvent
import com.waylo.app.domain.model.ProgressionResult
import com.waylo.app.domain.model.UserProgress
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import com.waylo.app.domain.model.WalkingSession
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ActivityDetailViewModelTest {

    private class FakeProgressionRepository : ProgressionRepository {
        val awardXp = MutableStateFlow<Int?>(null)
        val _awardEvent = MutableStateFlow<ProgressionAwardEvent?>(null)
        var consumeCalls = 0

        override val progress: Flow<UserProgress> = flowOf(UserProgress.empty())
        override val awardEvent: StateFlow<ProgressionAwardEvent?> = _awardEvent.asStateFlow()

        override suspend fun awardActivityXp(activityId: Long): ProgressionResult =
            error("not used by ActivityDetailViewModel")

        override suspend fun recoverAwardIfNeeded(activityId: Long, sessionUpdatedMillis: Long) = Unit

        override fun observeAwardXp(activityId: Long): Flow<Int?> = awardXp

        override suspend fun deleteActivityCascade(
            activityId: Long,
            deleteWalkData: suspend () -> Boolean,
        ): Boolean = deleteWalkData()

        override fun consumeAwardEvent() {
            consumeCalls += 1
            _awardEvent.value = null
        }
    }

    private class FakeAchievementRepository : AchievementRepository {
        val events = MutableStateFlow<AchievementUnlockedEvent?>(null)
        var consumeCalls = 0
            private set

        override val definitions = AchievementCatalog.definitions

        override val snapshot = kotlinx.coroutines.flow.flowOf(
            AchievementSnapshot(
                context = AchievementContext.empty(),
                unlockedIds = emptySet(),
                unlockedAtById = emptyMap(),
            ),
        )

        override val summary = kotlinx.coroutines.flow.flowOf(
            AchievementSummary(unlockedCount = 0, totalCount = definitions.size),
        )

        override val unlockEvent: StateFlow<AchievementUnlockedEvent?> = events.asStateFlow()

        override suspend fun reconcile(activityId: Long?): List<String> = emptyList()

        override fun consumeUnlockEvent() {
            consumeCalls += 1
            events.value = null
        }
    }

    private class FakeWalkingRepository : WalkingRepository {
        val sessionFlow = MutableStateFlow<WalkingSession?>(null)
        var sessionRoute: WalkRoute = WalkRoute()
        var routeFailure = false
        var observeFailure = false
        var deleteResult = false
        var deleteCalls = 0

        override val status: StateFlow<WalkingStatus> = MutableStateFlow(WalkingStatus())
        override val route: StateFlow<WalkRoute> = MutableStateFlow(WalkRoute())
        override val lastCompletedSession: StateFlow<WalkingSession?> = MutableStateFlow(null)

        override fun observeCompletedSessions(): Flow<List<WalkingSession>> = flowOf(emptyList())

        override fun observeSession(id: Long): Flow<WalkingSession?> =
            if (observeFailure) flow { throw IllegalStateException("database down") } else sessionFlow

        override suspend fun routeForSession(sessionId: Long): WalkRoute {
            if (routeFailure) throw IllegalStateException("route load failed")
            return sessionRoute
        }

        override suspend fun deleteActivity(sessionId: Long): Boolean {
            deleteCalls += 1
            if (deleteResult) sessionFlow.value = null
            return deleteResult
        }

        override fun startWalk() = Unit
        override fun pauseWalk() = Unit
        override fun resumeWalk() = Unit
        override fun stopWalk() = Unit
        override fun retryTracking() = Unit
        override fun dismissCompleted() = Unit
        override fun attachService() = Unit
        override fun detachService() = Unit
        override fun reportError(message: String) = Unit
    }

    private val utc: ZoneId = ZoneId.of("UTC")
    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val repository = FakeWalkingRepository()
    private val now = epoch(LocalDateTime.of(2026, 10, 3, 12, 0))
    private var deletedCalls = 0

    private val completedSession = WalkingSession(
        id = 7L,
        state = WalkingState.Completed,
        startMillis = epoch(LocalDateTime.of(2026, 9, 28, 18, 42)),
        updatedMillis = epoch(LocalDateTime.of(2026, 9, 28, 19, 13)),
        distanceMeters = 1_500.0,
        activeMillis = 600_000L,
        walkStartStepCount = 5_000L,
        walkStepCount = 2_931L,
    )

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun loadsTheWalkWithPhaseSixStatisticsAndLabels() {
        repository.sessionFlow.value = completedSession
        repository.sessionRoute = WalkRoute(
            points = listOf(
                point(sequence = 0, latitude = 52.0, longitude = 13.0),
                point(sequence = 1, latitude = 52.01, longitude = 13.01),
            ),
        )

        val viewModel = createViewModel(weightKg = flowOf(70))
        val loaded = awaitLoaded(viewModel)

        assertEquals(7L, loaded.session.id)
        assertTrue(loaded.routeLoaded)
        assertFalse(loaded.routeUnavailable)
        assertEquals(listOf(0, 1), loaded.route.points.map { it.sequence })
        assertEquals("September 28, 2026", loaded.dateLabel)
        assertEquals("6:42 PM", loaded.startTimeLabel)
        assertEquals("7:13 PM", loaded.endTimeLabel)
        assertEquals(70, loaded.weightKg)

        val expected = WorkoutStatisticsCalculator.statistics(
            distanceMeters = completedSession.distanceMeters,
            activeMillis = completedSession.activeMillis,
            liveMovement = false,
            route = WalkRoute(),
            nowMillis = now,
            weightKg = 70.0,
            walkStartStepCount = completedSession.walkStartStepCount,
            currentSensorCount = null,
            finalWalkStepCount = completedSession.walkStepCount,
        )
        assertEquals(expected, loaded.statistics)
        assertEquals(400.0, loaded.statistics.averagePaceSecondsPerKm!!, 0.001)
        assertEquals(2.5, loaded.statistics.averageSpeedMetersPerSecond!!, 0.001)
        assertEquals(2_931L, loaded.statistics.walkSteps)
        assertEquals(73.5, loaded.statistics.estimatedCaloriesKcal!!, 0.001)
    }

    @Test
    fun caloriesStayUnavailableWithoutAConfiguredWeight() {
        repository.sessionFlow.value = completedSession

        val viewModel = createViewModel(weightKg = flowOf(null))
        val loaded = awaitLoaded(viewModel)

        assertNull(loaded.weightKg)
        assertNull(loaded.statistics.estimatedCaloriesKcal)
    }

    @Test
    fun missingWalkIsNotFound() {
        val viewModel = createViewModel()

        val state = awaitState(viewModel) { it == ActivityDetailUiState.NotFound }
        assertEquals(ActivityDetailUiState.NotFound, state)
    }

    @Test
    fun deletionWhileTheScreenIsOpenBecomesNotFound() {
        repository.sessionFlow.value = completedSession
        val viewModel = createViewModel()
        awaitLoaded(viewModel)

        repository.sessionFlow.value = null

        val state = awaitState(viewModel) { it == ActivityDetailUiState.NotFound }
        assertEquals(ActivityDetailUiState.NotFound, state)
    }

    @Test
    fun databaseFailureIsAnErrorStateAndRetryRecovers() {
        repository.observeFailure = true
        val viewModel = createViewModel()
        val error = awaitState(viewModel) { it == ActivityDetailUiState.Error }
        assertEquals(ActivityDetailUiState.Error, error)

        repository.observeFailure = false
        repository.sessionFlow.value = completedSession
        viewModel.retry()

        val loaded = awaitLoaded(viewModel)
        assertEquals(7L, loaded.session.id)
    }

    @Test
    fun routeFailureKeepsTheStatisticsVisibleAndMarksTheRouteUnavailable() {
        repository.sessionFlow.value = completedSession
        repository.routeFailure = true

        val viewModel = createViewModel()
        val loaded = awaitLoaded(viewModel)

        assertTrue(loaded.routeLoaded)
        assertTrue(loaded.routeUnavailable)
        assertTrue(loaded.route.isEmpty)
        assertEquals(1_500.0, loaded.statistics.distanceMeters, 0.001)
    }

    @Test
    fun anEmptyRouteIsMarkedUnavailable() {
        repository.sessionFlow.value = completedSession
        repository.sessionRoute = WalkRoute()

        val viewModel = createViewModel()
        val loaded = awaitLoaded(viewModel)

        assertTrue(loaded.routeLoaded)
        assertTrue(loaded.routeUnavailable)
    }

    @Test
    fun theCameraFitsTheRecordedRouteWithoutFollowing() {
        repository.sessionFlow.value = completedSession
        repository.sessionRoute = WalkRoute(
            points = listOf(
                point(sequence = 0, latitude = 52.0, longitude = 13.0),
                point(sequence = 1, latitude = 52.01, longitude = 13.01),
            ),
        )

        val viewModel = createViewModel()
        val loaded = awaitLoaded(viewModel)

        assertTrue(loaded.map.completionCameraApplied)
        assertFalse(loaded.map.followEnabled)
        val command = loaded.map.cameraCommand
        assertTrue(command is MapCameraCommand.FitBounds)
    }

    @Test
    fun aSinglePointRouteCentersInsteadOfFittingBounds() {
        repository.sessionFlow.value = completedSession
        repository.sessionRoute = WalkRoute(points = listOf(point(sequence = 0, latitude = 52.0)))

        val viewModel = createViewModel()
        val loaded = awaitLoaded(viewModel)

        val command = loaded.map.cameraCommand
        assertTrue(command is MapCameraCommand.MoveTo)
        assertEquals(
            WalkMapCameraPolicy.COMPLETED_ZOOM,
            (command as MapCameraCommand.MoveTo).zoom,
            0.001,
        )
    }

    @Test
    fun deleteSucceedsInvokesTheCallbackAndClearsTheWalk() {
        repository.sessionFlow.value = completedSession
        repository.deleteResult = true
        val viewModel = createViewModel()
        awaitLoaded(viewModel)

        viewModel.delete()

        awaitState(viewModel) { it == ActivityDetailUiState.NotFound }
        assertEquals(1, repository.deleteCalls)
        assertEquals(1, deletedCalls)
    }

    @Test
    fun deleteRefusesWithoutNavigatingAway() {
        repository.sessionFlow.value = completedSession
        repository.deleteResult = false
        val viewModel = createViewModel()
        awaitLoaded(viewModel)

        viewModel.delete()

        val state = awaitState(viewModel) { repository.deleteCalls == 1 }
        assertEquals(1, repository.deleteCalls)
        assertEquals(0, deletedCalls)
        assertTrue(state is ActivityDetailUiState.Loaded)
    }

    @Test
    fun mapStyleLifecycleIsHonestAboutLoadingAndFailures() {
        repository.sessionFlow.value = completedSession
        val viewModel = createViewModel()
        val loaded = awaitLoaded(viewModel)
        assertEquals(WalkMapLoadState.Loading, loaded.map.loadState)

        viewModel.onMapStyleLoaded()
        assertEquals(WalkMapLoadState.Ready, loaded(mapState(viewModel)))

        viewModel.onMapStyleFailed()
        assertEquals(WalkMapLoadState.Ready, loaded(mapState(viewModel)))

        viewModel.retryMapStyle()
        assertEquals(WalkMapLoadState.Loading, loaded(mapState(viewModel)))

        viewModel.onMapStyleLoaded()
        assertEquals(WalkMapLoadState.Ready, loaded(mapState(viewModel)))
    }

    @Test
    fun networkLossMakesTheMapUnavailableUntilItReturns() {
        val network = MutableStateFlow(true)
        repository.sessionFlow.value = completedSession
        val viewModel = createViewModel(networkStatus = network)
        awaitLoaded(viewModel)
        viewModel.onMapStyleLoaded()

        network.value = false
        assertEquals(WalkMapLoadState.Unavailable, loaded(mapState(viewModel)))

        network.value = true
        assertEquals(WalkMapLoadState.Ready, loaded(mapState(viewModel)))
    }

    @Test
    fun withoutProgressionTheXpFieldsStayEmpty() {
        repository.sessionFlow.value = completedSession

        val loaded = awaitLoaded(createViewModel())

        assertNull(loaded.xpAwarded)
        assertNull(loaded.levelUp)
    }

    @Test
    fun earnedXpIsShownWhenTheLedgerHasAnAwardForThisWalk() {
        repository.sessionFlow.value = completedSession
        val progression = FakeProgressionRepository()
        progression.awardXp.value = 150

        val loaded = awaitLoaded(createViewModel(progression = progression))

        assertEquals(150, loaded.xpAwarded)
    }

    @Test
    fun aLevelUpForThisWalkIsSurfacedOnceAndConsumed() {
        repository.sessionFlow.value = completedSession
        val progression = FakeProgressionRepository()
        progression.awardXp.value = 150
        progression._awardEvent.value = ProgressionAwardEvent(
            activityId = 7L,
            xpAwarded = 150,
            totalXp = 150,
            levelBefore = 1,
            levelAfter = 2,
        )

        val loaded = awaitLoaded(createViewModel(progression = progression))

        assertNotNull(loaded.levelUp)
        assertEquals(1, loaded.levelUp!!.levelBefore)
        assertEquals(2, loaded.levelUp!!.levelAfter)
        assertEquals(150, loaded.levelUp!!.xpAwarded)
        assertEquals(1, progression.consumeCalls)
        assertNull(progression._awardEvent.value)
    }

    @Test
    fun aLevelUpForAnotherWalkIsIgnoredAndLeftUnconsumed() {
        repository.sessionFlow.value = completedSession
        val progression = FakeProgressionRepository()
        progression._awardEvent.value = ProgressionAwardEvent(
            activityId = 99L,
            xpAwarded = 40,
            totalXp = 40,
            levelBefore = 1,
            levelAfter = 1,
        )

        val loaded = awaitLoaded(createViewModel(progression = progression))

        assertNull(loaded.levelUp)
        assertEquals(0, progression.consumeCalls)
        assertNotNull(progression._awardEvent.value)
    }

    @Test
    fun anAchievementUnlockForThisWalkIsSurfacedOnceAndConsumed() {
        repository.sessionFlow.value = completedSession
        val achievements = FakeAchievementRepository()
        achievements.events.value = AchievementUnlockedEvent(
            activityId = 7L,
            unlocks = listOf(
                AchievementUnlock("first_walk", 1_790_001_860_000L),
                AchievementUnlock("distance_1km", 1_790_001_860_000L),
            ),
            unlockedAtMillis = 1_790_001_860_000L,
        )

        val loaded = awaitLoaded(createViewModel(achievements = achievements))

        assertNotNull(loaded.achievementUnlock)
        assertEquals(listOf("First Walk", "Walk 1 km"), loaded.achievementUnlock!!.titles)
        assertEquals(2, loaded.achievementUnlock!!.count)
        assertTrue(loaded.achievementUnlock!!.isMultiple)
        assertEquals(1, achievements.consumeCalls)
        assertNull(achievements.events.value)
    }

    @Test
    fun anAchievementUnlockForAnotherWalkIsIgnoredAndLeftUnconsumed() {
        repository.sessionFlow.value = completedSession
        val achievements = FakeAchievementRepository()
        achievements.events.value = AchievementUnlockedEvent(
            activityId = 99L,
            unlocks = listOf(AchievementUnlock("first_walk", 1L)),
            unlockedAtMillis = 1L,
        )

        val loaded = awaitLoaded(createViewModel(achievements = achievements))

        assertNull(loaded.achievementUnlock)
        assertEquals(0, achievements.consumeCalls)
        assertNotNull(achievements.events.value)
    }

    private fun loaded(state: ActivityDetailUiState): WalkMapLoadState =
        (state as ActivityDetailUiState.Loaded).map.loadState

    private fun mapState(viewModel: ActivityDetailViewModel): ActivityDetailUiState =
        viewModel.uiState.value

    private fun createViewModel(
        networkStatus: Flow<Boolean> = flowOf(true),
        weightKg: Flow<Int?> = flowOf(null),
        progression: ProgressionRepository? = null,
        achievements: AchievementRepository? = null,
    ): ActivityDetailViewModel = ActivityDetailViewModel(
        repository = repository,
        activityId = 7L,
        now = { now },
        zoneId = { utc },
        networkStatus = networkStatus,
        weightKg = weightKg,
        progression = progression,
        achievements = achievements,
        onDeleted = { deletedCalls += 1 },
        stateScope = testScope,
    )

    private fun awaitLoaded(viewModel: ActivityDetailViewModel): ActivityDetailUiState.Loaded =
        awaitState(viewModel) { it is ActivityDetailUiState.Loaded } as ActivityDetailUiState.Loaded

    private fun awaitState(
        viewModel: ActivityDetailViewModel,
        predicate: (ActivityDetailUiState) -> Boolean,
    ): ActivityDetailUiState = runBlocking {
        withTimeout(15_000) {
            while (!predicate(viewModel.uiState.value)) delay(10)
            viewModel.uiState.value
        }
    }

    private fun point(
        sequence: Int,
        latitude: Double,
        longitude: Double = 13.0,
    ) = WalkingLocationPoint(
        sessionId = 7L,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = 1_000L + sequence,
        accuracyMeters = 5f,
        altitudeMeters = null,
        speedMps = null,
    )

    private fun epoch(localDateTime: LocalDateTime): Long =
        localDateTime.atZone(utc).toInstant().toEpochMilli()
}
