package com.waylo.app.data.walk

import com.waylo.app.domain.model.WalkRoute
import com.waylo.app.domain.model.WalkingSession
import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WalkingRepository {
    val status: StateFlow<WalkingStatus>
    val route: StateFlow<WalkRoute>
    val lastCompletedSession: StateFlow<WalkingSession?>

    fun observeCompletedSessions(): Flow<List<WalkingSession>>
    fun observeSession(id: Long): Flow<WalkingSession?>
    suspend fun routeForSession(sessionId: Long): WalkRoute
    suspend fun deleteActivity(sessionId: Long): Boolean

    fun startWalk()
    fun pauseWalk()
    fun resumeWalk()
    fun stopWalk()
    fun retryTracking()
    fun dismissCompleted()
    fun attachService()
    fun detachService()
    fun reportError(message: String)
}
