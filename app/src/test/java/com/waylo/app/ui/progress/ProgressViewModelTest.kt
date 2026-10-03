package com.waylo.app.ui.progress

import com.waylo.app.data.progression.ProgressionRepository
import com.waylo.app.domain.model.ProgressionAwardEvent
import com.waylo.app.domain.model.ProgressionResult
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressViewModelTest {

    private class FakeProgressionRepository(
        initial: UserProgress = UserProgress.empty(),
    ) : ProgressionRepository {
        private val _progress = MutableStateFlow(initial)
        private val _awardEvent = MutableStateFlow<ProgressionAwardEvent?>(null)

        override val progress: Flow<UserProgress> = _progress.asStateFlow()
        override val awardEvent: StateFlow<ProgressionAwardEvent?> = _awardEvent.asStateFlow()

        override suspend fun awardActivityXp(activityId: Long): ProgressionResult =
            error("not used by ProgressViewModel")

        override suspend fun recoverAwardIfNeeded(activityId: Long, sessionUpdatedMillis: Long) = Unit

        override fun observeAwardXp(activityId: Long): Flow<Int?> = flowOf(null)

        override suspend fun deleteActivityCascade(
            activityId: Long,
            deleteWalkData: suspend () -> Boolean,
        ): Boolean = deleteWalkData()

        override fun consumeAwardEvent() {
            _awardEvent.value = null
        }

        fun emit(progress: UserProgress) {
            _progress.value = progress
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @After
    fun tearDown() {
        testScope.cancel()
    }

    private fun createViewModel(
        progression: ProgressionRepository? = null,
    ): ProgressViewModel = ProgressViewModel(
        progression = progression,
        stateScope = testScope,
    )

    @Test
    fun initialStateReportsNoProgressYet() {
        val state = createViewModel().uiState.value

        assertEquals(1, state.progress.level)
        assertEquals(0, state.progress.totalXp)
        assertEquals(0, state.progress.xp)
        assertEquals(100, state.progress.xpToNextLevel)
        assertEquals(0, state.progress.xpProgressPercent)
        assertEquals(0, state.progress.currentStreakDays)
        assertEquals(0, state.progress.longestStreakDays)
        assertEquals(0, state.progress.totalSteps)
        assertEquals(0.0, state.progress.totalDistanceKm, 0.0)
        assertEquals(0, state.progress.totalWalkingMinutes)
    }

    @Test
    fun uiStateMirrorsPersistedProgression() {
        val fake = FakeProgressionRepository(
            UserProgress(
                level = 3,
                totalXp = 500,
                xp = 100,
                xpToNextLevel = 900,
                currentStreakDays = 4,
                longestStreakDays = 9,
                totalSteps = 12_345,
                totalDistanceKm = 8.5,
                totalWalkingMinutes = 84,
            ),
        )
        val viewModel = createViewModel(fake)

        val state = viewModel.uiState.value

        assertEquals(3, state.progress.level)
        assertEquals(500, state.progress.totalXp)
        assertEquals(100, state.progress.xp)
        assertEquals(900, state.progress.xpToNextLevel)
        assertEquals(4, state.progress.currentStreakDays)
        assertEquals(9, state.progress.longestStreakDays)
        assertEquals(12_345, state.progress.totalSteps)
        assertEquals(8.5, state.progress.totalDistanceKm, 0.001)
        assertEquals(84, state.progress.totalWalkingMinutes)
    }

    @Test
    fun uiStateTracksLaterProgressionEmissions() {
        val fake = FakeProgressionRepository()
        val viewModel = createViewModel(fake)

        assertEquals(1, viewModel.uiState.value.progress.level)

        fake.emit(
            UserProgress(
                level = 2,
                totalXp = 150,
                xp = 50,
                xpToNextLevel = 400,
                currentStreakDays = 1,
                longestStreakDays = 1,
                totalSteps = 2_000,
                totalDistanceKm = 1.5,
                totalWalkingMinutes = 10,
            ),
        )

        val state = viewModel.uiState.value
        assertEquals(2, state.progress.level)
        assertEquals(150, state.progress.totalXp)
        assertEquals(50, state.progress.xp)
        assertEquals(1, state.progress.currentStreakDays)
    }
}
