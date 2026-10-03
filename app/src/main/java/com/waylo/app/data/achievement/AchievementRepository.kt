package com.waylo.app.data.achievement

import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementSnapshot
import com.waylo.app.domain.model.AchievementSummary
import com.waylo.app.domain.model.AchievementUnlockedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local-first achievement persistence and evaluation.
 *
 * Lifecycle of an unlock:
 * ```
 * persisted activity/progression aggregates
 *         ↓  (one AchievementContext, summarized once)
 * AchievementEvaluator          (pure, deterministic)
 *         ↓
 * insert-if-absent into Room    (idempotent)
 *         ↓  (only rows actually inserted)
 * one-shot AchievementUnlockedEvent → UI + mascot celebration
 * ```
 */
interface AchievementRepository {

    /** Immutable definitions evaluated against local data. */
    val definitions: List<AchievementDefinition>

    /** Live view of context + unlocked ids, re-derived from Room aggregates. */
    val snapshot: Flow<AchievementSnapshot>

    /** Compact unlocked/total counts for Profile and Progress. */
    val summary: Flow<AchievementSummary>

    /** One-shot event; only ever set for newly inserted unlocks. */
    val unlockEvent: StateFlow<AchievementUnlockedEvent?>

    /**
     * Evaluate all definitions against current data and persist every newly
     * satisfied achievement in one transaction. Safe to call repeatedly:
     * already-unlocked achievements insert nothing and emit no event.
     *
     * @param activityId walk that triggered this evaluation, or null at startup.
     * @return ids that were unlocked by *this* call.
     */
    suspend fun reconcile(activityId: Long? = null): List<String>

    /** Consume the pending unlock event so it is presented exactly once. */
    fun consumeUnlockEvent()
}
