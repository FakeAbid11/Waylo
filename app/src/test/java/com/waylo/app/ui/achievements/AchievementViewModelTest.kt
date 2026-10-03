package com.waylo.app.ui.achievements

import com.waylo.app.core.common.AchievementCatalog
import com.waylo.app.data.achievement.AchievementRepository
import com.waylo.app.domain.model.AchievementCategory
import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementSnapshot
import com.waylo.app.domain.model.AchievementSummary
import com.waylo.app.domain.model.AchievementUnlock
import com.waylo.app.domain.model.AchievementUnlockedEvent
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementViewModelTest {

    private class FakeAchievementRepository(
        definitions: List<AchievementDefinition> = AchievementCatalog.definitions,
        initial: AchievementSnapshot? = null,
    ) : AchievementRepository {
        private val snapshotFlow = MutableStateFlow(initial)
        private val eventFlow = MutableStateFlow<AchievementUnlockedEvent?>(null)

        var broken: Boolean = false
        var consumeCalls: Int = 0
            private set

        override val definitions: List<AchievementDefinition> = definitions

        override val snapshot: Flow<AchievementSnapshot>
            get() = if (broken) flowWithError() else snapshotFlow.filterNotNull()

        override val summary: Flow<AchievementSummary> = snapshotFlow.map { value ->
            AchievementSummary(
                unlockedCount = definitions.count { it.id in (value?.unlockedIds ?: emptySet()) },
                totalCount = definitions.size,
            )
        }

        override val unlockEvent: StateFlow<AchievementUnlockedEvent?> = eventFlow.asStateFlow()

        override suspend fun reconcile(activityId: Long?): List<String> = emptyList()

        override fun consumeUnlockEvent() {
            consumeCalls += 1
            eventFlow.value = null
        }

        fun emit(snapshot: AchievementSnapshot) {
            snapshotFlow.value = snapshot
        }

        fun emitEvent(event: AchievementUnlockedEvent) {
            eventFlow.value = event
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @After
    fun tearDown() {
        testScope.cancel()
    }

    private fun createViewModel(
        repository: AchievementRepository,
    ): AchievementViewModel = AchievementViewModel(
        repository = repository,
        zoneId = { ZoneOffset.UTC },
        stateScope = testScope,
    )

    private fun snapshot(
        walks: Int = 0,
        distanceMeters: Long = 0L,
        xp: Long = 0L,
        level: Int = 1,
        longestStreak: Int = 0,
        steps: Long = 0L,
        unlockedAt: Map<String, Long> = emptyMap(),
    ) = AchievementSnapshot(
        context = AchievementContext(
            completedWalkCount = walks,
            totalDistanceMeters = distanceMeters,
            totalXp = xp,
            level = level,
            currentStreakDays = longestStreak,
            longestStreakDays = longestStreak,
            totalSteps = steps,
            totalActiveSeconds = 0L,
        ),
        unlockedIds = unlockedAt.keys,
        unlockedAtById = unlockedAt,
    )

    @Test
    fun stateStartsLoadingUntilTheFirstSnapshotArrives() {
        val fake = FakeAchievementRepository()
        val viewModel = createViewModelWithNeverEmittingSnapshot(fake)

        val state = viewModel.uiState.value
        assertTrue(state.loading)
        assertFalse(state.error)
        assertTrue(state.achievements.isEmpty())
    }

    @Test
    fun rendersLockedAndUnlockedCardsWithCounts() {
        val fake = FakeAchievementRepository(
            initial = snapshot(
                walks = 1,
                distanceMeters = 1_240L,
                unlockedAt = mapOf(
                    "first_walk" to 1_790_000_000_000L,
                    "distance_1km" to 1_790_000_000_000L,
                ),
            ),
        )
        val viewModel = createViewModel(fake)

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(AchievementCatalog.definitions.size, state.totalCount)
        assertEquals(2, state.unlockedCount)
        assertEquals(AchievementCatalog.definitions.size, state.achievements.size)

        val firstWalk = state.achievements.first { it.id == "first_walk" }
        assertTrue(firstWalk.unlocked)
        assertEquals("Unlocked", firstWalk.statusLabel)
        assertEquals("September 21, 2026", firstWalk.dateLabel)
        assertNull(firstWalk.progressText)
        assertEquals(
            "Complete your first walk. Completed on September 21, 2026.",
            firstWalk.accessibilityText,
        )

        val tenKm = state.achievements.first { it.id == "distance_10km" }
        assertFalse(tenKm.unlocked)
        assertEquals("Locked", tenKm.statusLabel)
        assertNull(tenKm.dateLabel)
        assertEquals("1.24 km / 10.00 km", tenKm.progressText)
    }

    @Test
    fun lockedProgressShowsRealTotalsNeverFakeNumbers() {
        val fake = FakeAchievementRepository(initial = snapshot(distanceMeters = 1_240L))
        val viewModel = createViewModel(fake)

        val tenKm = viewModel.uiState.value.achievements.first { it.id == "distance_10km" }

        assertEquals("1.24 km / 10.00 km", tenKm.progressText)
        assertEquals(0.124, tenKm.progressFraction.toDouble(), 0.0001)
        assertEquals(
            "Walk a total of 10 kilometers. Locked. Progress: " +
                "1.24 kilometers of 10 kilometers.",
            tenKm.accessibilityText,
        )
    }

    @Test
    fun xpStepAndStreakProgressRenderWithTheirUnits() {
        val fake = FakeAchievementRepository(
            initial = snapshot(xp = 720L, steps = 8_500L, longestStreak = 2, walks = 3),
        )
        val achievements = createViewModel(fake).uiState.value.achievements

        assertEquals("720 / 1,000 XP", achievements.first { it.id == "xp_1000" }.progressText)
        assertEquals(
            "8,500 / 10,000 steps",
            achievements.first { it.id == "steps_10000" }.progressText,
        )
        assertEquals("2 / 3 days", achievements.first { it.id == "streak_3" }.progressText)
        assertEquals("3 / 10 walks", achievements.first { it.id == "walks_10" }.progressText)
    }

    @Test
    fun filteringByCategoryNarrowsTheVisibleCards() {
        val fake = FakeAchievementRepository(initial = snapshot(walks = 1))
        val viewModel = createViewModel(fake)
        assertEquals(AchievementCatalog.definitions.size, viewModel.uiState.value.achievements.size)

        viewModel.setFilter(AchievementCategory.DISTANCE)
        val filtered = viewModel.uiState.value
        assertEquals(AchievementCategory.DISTANCE, filtered.filter)
        assertTrue(filtered.achievements.isNotEmpty())
        assertTrue(filtered.achievements.all { it.category == AchievementCategory.DISTANCE })

        viewModel.setFilter(null)
        assertEquals(
            AchievementCatalog.definitions.size,
            viewModel.uiState.value.achievements.size,
        )
    }

    @Test
    fun emptyStateFlagsDescribeAllLockedAndAllUnlocked() {
        val locked = createViewModel(FakeAchievementRepository(initial = snapshot()))
        assertTrue(locked.uiState.value.allLocked)
        assertFalse(locked.uiState.value.allUnlocked)

        val everything = AchievementCatalog.definitions
            .associate { it.id to 1_790_000_000_000L }
        val unlocked = createViewModel(
            FakeAchievementRepository(initial = snapshot(unlockedAt = everything)),
        )
        assertTrue(unlocked.uiState.value.allUnlocked)
        assertFalse(unlocked.uiState.value.allLocked)
    }

    @Test
    fun aDatabaseFailureBecomesAnErrorStateAndRetryRecovers() {
        val fake = FakeAchievementRepository(
            initial = snapshot(walks = 1, unlockedAt = mapOf("first_walk" to 1_790_000_000_000L)),
        )
        val viewModel = createViewModel(fake)

        fake.broken = true
        viewModel.retry()
        val error = viewModel.uiState.value
        assertTrue(error.error)
        assertFalse(error.loading)
        assertTrue(error.achievements.isEmpty())

        fake.broken = false
        viewModel.retry()
        val recovered = viewModel.uiState.value
        assertFalse(recovered.error)
        assertFalse(recovered.loading)
        assertEquals(1, recovered.unlockedCount)
    }

    @Test
    fun theUnlockBannerIsShownOnceAndConsumedOnDismiss() {
        val fake = FakeAchievementRepository(initial = snapshot(walks = 1))
        val viewModel = createViewModel(fake)
        assertNull(viewModel.uiState.value.unlockBanner)

        fake.emitEvent(
            AchievementUnlockedEvent(
                activityId = 7L,
                unlocks = listOf(
                    AchievementUnlock("first_walk", 1_790_000_000_000L),
                    AchievementUnlock("distance_1km", 1_790_000_000_000L),
                ),
                unlockedAtMillis = 1_790_000_000_000L,
            ),
        )

        val banner = viewModel.uiState.value.unlockBanner
        assertNotNull(banner)
        assertEquals(2, banner!!.count)
        assertTrue(banner.isMultiple)
        assertEquals(listOf("First Walk", "Walk 1 km"), banner.titles)

        viewModel.dismissUnlockBanner()

        assertEquals(1, fake.consumeCalls)
        assertNull(viewModel.uiState.value.unlockBanner)

        // Re-collecting after consumption never resurrects the presentation.
        repeat(3) { assertNull(viewModel.uiState.value.unlockBanner) }
        assertEquals(1, fake.consumeCalls)
    }

    @Test
    fun unknownIdsInAnUnlockEventProduceNoBanner() {
        val fake = FakeAchievementRepository(initial = snapshot())
        val viewModel = createViewModel(fake)

        fake.emitEvent(
            AchievementUnlockedEvent(
                activityId = null,
                unlocks = listOf(AchievementUnlock("removed_in_a_future_version", 1L)),
                unlockedAtMillis = 1L,
            ),
        )

        assertNull(viewModel.uiState.value.unlockBanner)
    }

    @Test
    fun progressTextAndSpokenProgressCoverEveryRequirementShape() {
        assertEquals(
            "999 m / 1.00 km",
            AchievementViewModel.progressText(
                com.waylo.app.domain.model.AchievementRequirement.DistanceMeters(1_000L),
                current = 999L,
                target = 1_000L,
            ),
        )
        assertEquals(
            "Level 4 / 5",
            AchievementViewModel.progressText(
                com.waylo.app.domain.model.AchievementRequirement.Level(5),
                current = 4L,
                target = 5L,
            ),
        )
        assertEquals(
            "30 / 60 min",
            AchievementViewModel.progressText(
                com.waylo.app.domain.model.AchievementRequirement.ActiveDurationSeconds(3_600L),
                current = 1_800L,
                target = 3_600L,
            ),
        )
        assertEquals(
            "Progress: 10 kilometers of 10 kilometers.",
            AchievementViewModel.spokenProgress(
                com.waylo.app.domain.model.AchievementRequirement.DistanceMeters(10_000L),
                current = 10_000L,
                target = 10_000L,
            ),
        )
        assertEquals(
            "Progress: level 4 of 5.",
            AchievementViewModel.spokenProgress(
                com.waylo.app.domain.model.AchievementRequirement.Level(5),
                current = 4L,
                target = 5L,
            ),
        )
        assertEquals("999 meters", AchievementViewModel.spokenDistance(999L))
        assertEquals("10 kilometers", AchievementViewModel.spokenDistance(10_000L))
        assertEquals("1.24 kilometers", AchievementViewModel.spokenDistance(1_240L))
    }

    private fun createViewModelWithNeverEmittingSnapshot(
        repository: AchievementRepository,
    ): AchievementViewModel {
        val holder = object : AchievementRepository by repository {
            override val snapshot: Flow<AchievementSnapshot> = emptyFlow()
        }
        return createViewModel(holder)
    }
}

private fun flowWithError(): Flow<AchievementSnapshot> = flow {
    throw IllegalStateException("database down")
}
