package com.waylo.app.data.walk

import com.waylo.app.core.common.WalkingEngine
import com.waylo.app.core.permissions.PermissionState
import com.waylo.app.data.local.WalkingDao
import com.waylo.app.data.local.WalkingLocationPointEntity
import com.waylo.app.data.local.WalkingSessionEntity
import com.waylo.app.data.location.LocationDataSource
import com.waylo.app.domain.model.LocationSample
import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingLocationPoint
import com.waylo.app.domain.model.WalkingSession
import com.waylo.app.domain.model.WalkingState
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WalkingRepositoryImpl(
    private val locationDataSource: LocationDataSource,
    private val dao: WalkingDao,
    private val permissionState: () -> PermissionState,
    private val now: () -> Long,
    private val measureDistance: (LocationSample, LocationSample) -> Double,
    private val stepCountNow: () -> Long? = { null },
    private val scope: CoroutineScope,
) : WalkingRepository {

    private val _status = MutableStateFlow(WalkingStatus())
    override val status: StateFlow<WalkingStatus> = _status.asStateFlow()

    private val _route = MutableStateFlow(WalkRoute())
    override val route: StateFlow<WalkRoute> = _route.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<WalkingSession?>(null)
    override val lastCompletedSession: StateFlow<WalkingSession?> =
        _lastCompletedSession.asStateFlow()

    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private var session: WalkingSession? = null
    private var engine: WalkingEngine? = null
    private var nextPointSequence = 0
    private var tracking = false

    init {
        scope.launch {
            mutex.withLock {
                restoreSession()
                loaded.complete(Unit)
            }
        }
    }

    override fun startWalk() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session
                if (current != null && current.state != WalkingState.Completed) return@withLock
                val start = now()
                val created = WalkingSession(
                    state = WalkingState.Starting,
                    startMillis = start,
                    updatedMillis = start,
                    walkStartStepCount = captureWalkStartStepCount(),
                )
                val id = dao.insertSession(created.toEntity())
                session = created.copy(id = id)
                engine = WalkingEngine(measureDistance)
                nextPointSequence = 0
                _route.value = WalkRoute()
                emitStatus()
                beginTracking()
            }
        }
    }

    override fun pauseWalk() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                if (current.state != WalkingState.Active) return@withLock
                val at = now()
                locationDataSource.stop()
                tracking = false
                val paused = current.copy(
                    state = WalkingState.Paused,
                    activeMillis = foldActive(current, at),
                    activeSegmentStartMillis = null,
                    pausedSegmentStartMillis = at,
                    updatedMillis = at,
                )
                session = paused
                persist(paused)
                _route.value = _route.value.markBreakBeforeNextPoint()
                emitStatus()
            }
        }
    }

    override fun resumeWalk() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                if (current.state != WalkingState.Paused) return@withLock
                val at = now()
                session = current.copy(
                    pausedMillis = foldPaused(current, at),
                    pausedSegmentStartMillis = null,
                    updatedMillis = at,
                )
                beginTracking()
            }
        }
    }

    override fun stopWalk() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                if (current.state != WalkingState.Active &&
                    current.state != WalkingState.Paused &&
                    current.state != WalkingState.Starting &&
                    current.state != WalkingState.Error
                ) {
                    return@withLock
                }
                val stopAt = now()
                val stopping = current.copy(state = WalkingState.Stopping, updatedMillis = stopAt)
                session = stopping
                emitStatus()
                persist(stopping)
                locationDataSource.stop()
                tracking = false
                val completed = stopping.copy(
                    state = WalkingState.Completed,
                    activeMillis = foldActive(stopping, stopAt),
                    activeSegmentStartMillis = null,
                    pausedMillis = foldPaused(stopping, stopAt),
                    pausedSegmentStartMillis = null,
                    updatedMillis = stopAt,
                    errorMessage = null,
                    walkStepCount = finalWalkStepCount(stopping),
                )
                session = completed
                persist(completed)
                emitStatus()
                _lastCompletedSession.value = completed
            }
        }
    }

    override fun retryTracking() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                if (current.state != WalkingState.Error) return@withLock
                beginTracking()
            }
        }
    }

    override fun dismissCompleted() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                if (session?.state != WalkingState.Completed) return@withLock
                session = null
                engine = null
                _route.value = WalkRoute()
                emitStatus()
            }
        }
    }

    override fun observeCompletedSessions(): Flow<List<WalkingSession>> =
        dao.observeCompletedSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeSession(id: Long): Flow<WalkingSession?> =
        dao.observeSession(id).map { row -> row?.toDomain() }

    override suspend fun routeForSession(sessionId: Long): WalkRoute = WalkRoute(
        points = dao.locationPoints(sessionId).mapNotNull { row ->
            row.toDomain().takeIf { it.hasPlottablePosition() }
        },
    )

    override suspend fun deleteActivity(sessionId: Long): Boolean = mutex.withLock {
        loaded.await()
        val removed = dao.deleteActivity(sessionId)
        if (removed && _lastCompletedSession.value?.id == sessionId) {
            _lastCompletedSession.value = null
        }
        removed
    }

    override fun attachService() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                val canTrack = current.state == WalkingState.Starting || current.state == WalkingState.Active
                if (canTrack && !tracking) {
                    beginTracking()
                }
            }
        }
    }

    override fun detachService() {
        scope.launch {
            mutex.withLock {
                loaded.await()
                if (tracking) {
                    fail(INTERRUPTED_MESSAGE)
                }
            }
        }
    }

    override fun reportError(message: String) {
        scope.launch {
            mutex.withLock {
                loaded.await()
                fail(message)
            }
        }
    }

    private suspend fun beginTracking() {
        val current = session ?: return
        if (!hasFineLocationPermission()) {
            fail(PERMISSION_MESSAGE)
            return
        }
        if (!locationDataSource.isLocationEnabled()) {
            fail(DISABLED_MESSAGE)
            return
        }
        engine?.resetAnchor()
        if (!locationDataSource.start(::onLocationSample)) {
            fail(UNAVAILABLE_MESSAGE)
            return
        }
        tracking = true
        val start = now()
        val active = current.copy(
            state = WalkingState.Active,
            activeSegmentStartMillis = start,
            updatedMillis = start,
            errorMessage = null,
        )
        session = active
        persist(active)
        emitStatus()
    }

    private fun onLocationSample(sample: LocationSample) {
        scope.launch {
            mutex.withLock {
                loaded.await()
                val current = session ?: return@withLock
                val activeEngine = engine ?: return@withLock
                if (current.state != WalkingState.Active || !tracking) return@withLock
                if (!activeEngine.submit(sample)) return@withLock
                val at = now()
                val sequence = nextPointSequence
                nextPointSequence += 1
                val updated = current.copy(
                    distanceMeters = activeEngine.distanceMeters,
                    startLatitude = current.startLatitude ?: sample.latitude,
                    startLongitude = current.startLongitude ?: sample.longitude,
                    lastLatitude = sample.latitude,
                    lastLongitude = sample.longitude,
                    updatedMillis = at,
                )
                session = updated
                val point = WalkingLocationPoint(
                    sessionId = updated.id,
                    sequence = sequence,
                    latitude = sample.latitude,
                    longitude = sample.longitude,
                    timestampMillis = sample.timestampMillis,
                    accuracyMeters = sample.accuracyMeters,
                    altitudeMeters = sample.altitudeMeters,
                    speedMps = sample.speedMps,
                )
                dao.recordProgress(updated.toEntity(), point.toEntity())
                _route.value = _route.value.withPoint(point)
                emitStatus()
            }
        }
    }

    private suspend fun fail(message: String) {
        val current = session ?: return
        if (current.state != WalkingState.Starting &&
            current.state != WalkingState.Active &&
            current.state != WalkingState.Paused
        ) {
            return
        }
        locationDataSource.stop()
        tracking = false
        val at = now()
        val failed = current.copy(
            state = WalkingState.Error,
            errorMessage = message,
            activeMillis = foldActive(current, at),
            activeSegmentStartMillis = null,
            pausedMillis = foldPaused(current, at),
            pausedSegmentStartMillis = null,
            updatedMillis = at,
        )
        session = failed
        persist(failed)
        emitStatus()
    }

    private suspend fun restoreSession() {
        val persisted = dao.latestSession() ?: return
        val restored = persisted.toDomain()
        when (restored.state) {
            WalkingState.Starting, WalkingState.Active, WalkingState.Paused -> recover(restored)
            WalkingState.Stopping -> {
                val at = now()
                val completed = closeSegments(restored, at).copy(
                    state = WalkingState.Completed,
                    updatedMillis = at,
                    errorMessage = null,
                )
                persist(completed)
                _lastCompletedSession.value = completed
            }
            WalkingState.Error -> {
                session = restored
                engine = WalkingEngine(measureDistance, restored.distanceMeters)
                _route.value = loadRoute(restored.id)
                emitStatus()
            }
            WalkingState.Completed -> _lastCompletedSession.value = restored
            WalkingState.Idle -> Unit
        }
    }

    private suspend fun recover(recovered: WalkingSession) {
        val at = now()
        val folded = closeSegments(recovered, recovered.updatedMillis).copy(updatedMillis = at)
        session = folded
        engine = WalkingEngine(measureDistance, folded.distanceMeters)
        _route.value = loadRoute(folded.id)
        persist(folded)
        emitStatus()
    }

    private suspend fun loadRoute(sessionId: Long): WalkRoute = WalkRoute(
        points = dao.locationPoints(sessionId).map { it.toDomain() },
    )

    private fun closeSegments(value: WalkingSession, atMillis: Long): WalkingSession = value.copy(
        activeMillis = foldActive(value, atMillis),
        activeSegmentStartMillis = null,
        pausedMillis = foldPaused(value, atMillis),
        pausedSegmentStartMillis = null,
    )

    private fun foldActive(value: WalkingSession, atMillis: Long): Long {
        val segmentStart = value.activeSegmentStartMillis ?: return value.activeMillis
        return value.activeMillis + (atMillis - segmentStart).coerceAtLeast(0L)
    }

    private fun foldPaused(value: WalkingSession, atMillis: Long): Long {
        val segmentStart = value.pausedSegmentStartMillis ?: return value.pausedMillis
        return value.pausedMillis + (atMillis - segmentStart).coerceAtLeast(0L)
    }

    private fun hasFineLocationPermission(): Boolean {
        val permission = permissionState()
        return permission == PermissionState.Granted || permission == PermissionState.Unsupported
    }

    private fun captureWalkStartStepCount(): Long? = stepCountNow()?.takeIf { it > 0L }

    private fun finalWalkStepCount(stopping: WalkingSession): Long? {
        val baseline = stopping.walkStartStepCount ?: return null
        val current = stepCountNow() ?: return null
        if (current < baseline) return null
        return (current - baseline).coerceAtLeast(0L)
    }

    private suspend fun persist(value: WalkingSession) {
        if (value.id == 0L) return
        dao.updateSession(value.toEntity())
    }

    private fun emitStatus() {
        val current = session
        _status.value = if (current == null) {
            WalkingStatus()
        } else {
            WalkingStatus(
                state = current.state,
                sessionId = current.id,
                distanceMeters = current.distanceMeters,
                activeMillis = current.activeMillis,
                activeSegmentStartMillis = current.activeSegmentStartMillis,
                startedAtMillis = current.startMillis,
                updatedAtMillis = current.updatedMillis,
                hasFix = current.hasFix,
                errorMessage = current.errorMessage,
                walkStartStepCount = current.walkStartStepCount,
                walkStepCount = current.walkStepCount,
            )
        }
    }

    private companion object {
        const val PERMISSION_MESSAGE = "Location permission is needed to record this walk."
        const val DISABLED_MESSAGE = "Location services are turned off."
        const val UNAVAILABLE_MESSAGE = "Location isn't available on this device."
        const val INTERRUPTED_MESSAGE = "Walk tracking was interrupted."
    }
}

private fun WalkingSessionEntity.toDomain(): WalkingSession = WalkingSession(
    id = id,
    state = WalkingState.entries.firstOrNull { it.name == state } ?: WalkingState.Error,
    startMillis = startMillis,
    updatedMillis = updatedMillis,
    distanceMeters = distanceMeters,
    activeMillis = activeMillis,
    pausedMillis = pausedMillis,
    activeSegmentStartMillis = activeSegmentStartMillis,
    pausedSegmentStartMillis = pausedSegmentStartMillis,
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    lastLatitude = lastLatitude,
    lastLongitude = lastLongitude,
    errorMessage = errorMessage,
    walkStartStepCount = walkStartStepCount,
    walkStepCount = walkStepCount,
)

private fun WalkingSession.toEntity(): WalkingSessionEntity = WalkingSessionEntity(
    id = id,
    state = state.name,
    startMillis = startMillis,
    updatedMillis = updatedMillis,
    distanceMeters = distanceMeters,
    activeMillis = activeMillis,
    pausedMillis = pausedMillis,
    activeSegmentStartMillis = activeSegmentStartMillis,
    pausedSegmentStartMillis = pausedSegmentStartMillis,
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    lastLatitude = lastLatitude,
    lastLongitude = lastLongitude,
    errorMessage = errorMessage,
    walkStartStepCount = walkStartStepCount,
    walkStepCount = walkStepCount,
)

private fun WalkingLocationPoint.toEntity(): WalkingLocationPointEntity = WalkingLocationPointEntity(
    sessionId = sessionId,
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    timestampMillis = timestampMillis,
    accuracyMeters = accuracyMeters,
    altitudeMeters = altitudeMeters,
    speedMps = speedMps,
)

private fun WalkingLocationPointEntity.toDomain(): WalkingLocationPoint = WalkingLocationPoint(
    sessionId = sessionId,
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    timestampMillis = timestampMillis,
    accuracyMeters = accuracyMeters,
    altitudeMeters = altitudeMeters,
    speedMps = speedMps,
)

private fun WalkingLocationPoint.hasPlottablePosition(): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0
