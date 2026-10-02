package com.waylo.app.data.walk

import com.waylo.app.domain.model.WalkingStatus
import kotlinx.coroutines.flow.StateFlow

interface WalkingRepository {
    val status: StateFlow<WalkingStatus>

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
