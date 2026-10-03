package com.waylo.app.ui.history

import com.waylo.app.data.walk.WalkingRepository
import com.waylo.app.domain.model.WalkRoute
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ActivityHistoryViewModelTest {

    private class FakeWalkingRepository : WalkingRepository {
        var history: Flow<List<WalkingSession>> = flowOf(emptyList())

        override val status: StateFlow<WalkingStatus> = MutableStateFlow(WalkingStatus())
        override val route: StateFlow<WalkRoute> = MutableStateFlow(WalkRoute())
        override val lastCompletedSession: StateFlow<WalkingSession?> = MutableStateFlow(null)

        override fun observeCompletedSessions(): Flow<List<WalkingSession>> = history
        override fun observeSession(id: Long): Flow<WalkingSession?> = flowOf(null)
        override suspend fun routeForSession(sessionId: Long): WalkRoute = WalkRoute()
        override suspend fun deleteActivity(sessionId: Long): Boolean = false

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
    private val now = epoch(LocalDateTime.of(2026, 10, 3, 21, 0))

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun startsLoadingThenReportsEmptyWhenThereAreNoWalks() {
        repository.history = flowOf(emptyList())

        val viewModel = createViewModel()

        val state = awaitState(viewModel) { it == ActivityHistoryUiState.Empty }
        assertEquals(ActivityHistoryUiState.Empty, state)
    }

    @Test
    fun groupsWalksIntoLocalDaysNewestFirst() {
        val today = epoch(LocalDateTime.of(2026, 10, 3, 18, 42))
        val yesterday = epoch(LocalDateTime.of(2026, 10, 2, 7, 5))
        val older = epoch(LocalDateTime.of(2026, 9, 28, 6, 42))
        repository.history = flowOf(
            listOf(
                session(id = 3, startMillis = today),
                session(id = 2, startMillis = yesterday),
                session(id = 1, startMillis = older),
            ),
        )

        val viewModel = createViewModel()
        val content = awaitState(viewModel) {
            it is ActivityHistoryUiState.Content
        } as ActivityHistoryUiState.Content

        assertEquals(listOf("Today", "Yesterday", "Sep 28, 2026"), content.sections.map { it.label })
        assertEquals(listOf(3L), content.sections[0].entries.map { it.id })
        assertEquals(listOf(2L), content.sections[1].entries.map { it.id })
        assertEquals(listOf(1L), content.sections[2].entries.map { it.id })
        assertEquals("Today · 6:42 PM", content.sections[0].entries[0].timestampLabel)
    }

    @Test
    fun rowValuesAreDerivedFromThePersistedSession() {
        repository.history = flowOf(
            listOf(
                session(
                    id = 4,
                    startMillis = epoch(LocalDateTime.of(2026, 10, 3, 18, 42)),
                    distanceMeters = 1_500.0,
                    activeMillis = 600_000L,
                    walkStepCount = 2_931L,
                ),
                session(
                    id = 5,
                    startMillis = epoch(LocalDateTime.of(2026, 10, 3, 8, 0)),
                    distanceMeters = 5.0,
                    activeMillis = 60_000L,
                    walkStepCount = null,
                ),
            ),
        )

        val viewModel = createViewModel()
        val content = awaitState(viewModel) {
            it is ActivityHistoryUiState.Content
        } as ActivityHistoryUiState.Content

        val rich = content.sections[0].entries[0]
        assertEquals(1_500.0, rich.distanceMeters, 0.001)
        assertEquals(600_000L, rich.activeMillis)
        assertEquals(400.0, rich.averagePaceSecondsPerKm!!, 0.001)
        assertEquals(2_931L, rich.walkSteps)

        val sparse = content.sections[0].entries[1]
        assertEquals(null, sparse.averagePaceSecondsPerKm)
        assertEquals(null, sparse.walkSteps)
    }

    @Test
    fun databaseFailureSurfacesAsAnErrorState() {
        repository.history = flow { throw IllegalStateException("database down") }

        val viewModel = createViewModel()

        val state = awaitState(viewModel) { it == ActivityHistoryUiState.Error }
        assertEquals(ActivityHistoryUiState.Error, state)
    }

    @Test
    fun retryResubscribesAfterAFailure() {
        repository.history = flow { throw IllegalStateException("database down") }
        val viewModel = createViewModel()
        awaitState(viewModel) { it == ActivityHistoryUiState.Error }

        repository.history = flowOf(listOf(session(id = 9, startMillis = now)))
        viewModel.retry()

        val state = awaitState(viewModel) { it is ActivityHistoryUiState.Content }
        assertTrue(state is ActivityHistoryUiState.Content)
    }

    @Test
    fun updatesWhenAnActivityIsDeleted() {
        val sessions = MutableStateFlow(
            listOf(
                session(id = 3, startMillis = now),
                session(id = 2, startMillis = now - 60_000L),
            ),
        )
        repository.history = sessions
        val viewModel = createViewModel()
        val first = awaitState(viewModel) { it is ActivityHistoryUiState.Content }
            as ActivityHistoryUiState.Content
        assertEquals(2, first.sections[0].entries.size)

        sessions.value = sessions.value.filterNot { it.id == 3L }
        val second = awaitState(viewModel) {
            it is ActivityHistoryUiState.Content &&
                it.sections.sumOf { section -> section.entries.size } == 1
        } as ActivityHistoryUiState.Content
        assertEquals(listOf(2L), second.sections[0].entries.map { it.id })

        sessions.value = emptyList()
        val third = awaitState(viewModel) { it == ActivityHistoryUiState.Empty }
        assertEquals(ActivityHistoryUiState.Empty, third)
    }

    @Test
    fun dayGroupingUsesTheInjectedZone() {
        val walk = epoch(LocalDateTime.of(2026, 10, 3, 1, 0))
        repository.history = flowOf(listOf(session(id = 6, startMillis = walk)))

        val viewModel = createViewModel(zoneId = ZoneId.of("America/New_York"))
        val content = awaitState(viewModel) {
            it is ActivityHistoryUiState.Content
        } as ActivityHistoryUiState.Content

        assertEquals("Yesterday", content.sections[0].label)
        assertEquals("Yesterday · 9:00 PM", content.sections[0].entries[0].timestampLabel)
    }

    private fun createViewModel(
        zoneId: ZoneId = utc,
    ): ActivityHistoryViewModel = ActivityHistoryViewModel(
        repository = repository,
        now = { now },
        zoneId = { zoneId },
        stateScope = testScope,
    )

    private fun awaitState(
        viewModel: ActivityHistoryViewModel,
        predicate: (ActivityHistoryUiState) -> Boolean,
    ): ActivityHistoryUiState = runBlocking {
        withTimeout(15_000) {
            while (!predicate(viewModel.uiState.value)) delay(10)
            viewModel.uiState.value
        }
    }

    private fun session(
        id: Long,
        startMillis: Long,
        distanceMeters: Double = 2_840.0,
        activeMillis: Long = 1_884_000L,
        walkStepCount: Long? = 2_931L,
    ) = WalkingSession(
        id = id,
        state = WalkingState.Completed,
        startMillis = startMillis,
        updatedMillis = startMillis + activeMillis,
        distanceMeters = distanceMeters,
        activeMillis = activeMillis,
        walkStepCount = walkStepCount,
    )

    private fun epoch(localDateTime: LocalDateTime): Long =
        localDateTime.atZone(utc).toInstant().toEpochMilli()
}
