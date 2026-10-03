package com.waylo.app.data.progression

import com.waylo.app.domain.model.ProgressionAwardEvent
import com.waylo.app.domain.model.ProgressionResult
import com.waylo.app.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ProgressionRepository {

    val progress: Flow<UserProgress>

    val awardEvent: StateFlow<ProgressionAwardEvent?>

    suspend fun awardActivityXp(activityId: Long): ProgressionResult

    suspend fun recoverAwardIfNeeded(activityId: Long, sessionUpdatedMillis: Long)

    fun observeAwardXp(activityId: Long): Flow<Int?>

    suspend fun deleteActivityCascade(
        activityId: Long,
        deleteWalkData: suspend () -> Boolean,
    ): Boolean

    fun consumeAwardEvent()
}
