package com.waylo.app.core.common

import com.waylo.app.domain.model.AchievementContext
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementRequirement

/**
 * Pure Kotlin achievement evaluation. No Android, Room or Compose dependencies.
 *
 * Rules:
 * - an achievement is satisfied when `current >= required` (never equality), so
 *   a 10.42 km lifetime satisfies a 10 km milestone;
 * - evaluation is deterministic: the same context always yields the same result;
 * - all values are whole numbers (meters, XP, steps, seconds) — no floating point
 *   comparison is ever used for a threshold.
 */
object AchievementEvaluator {

    /** The user's current value for a requirement, from the summarized context. */
    fun currentValue(requirement: AchievementRequirement, context: AchievementContext): Long =
        when (requirement) {
            is AchievementRequirement.CompletedWalks -> context.completedWalkCount.toLong()
            is AchievementRequirement.DistanceMeters -> context.totalDistanceMeters
            is AchievementRequirement.TotalXp -> context.totalXp
            is AchievementRequirement.Level -> context.level.toLong()
            is AchievementRequirement.StreakDays -> context.longestStreakDays.toLong()
            is AchievementRequirement.Steps -> context.totalSteps
            is AchievementRequirement.ActiveDurationSeconds -> context.totalActiveSeconds
        }

    /** The milestone value a requirement asks for. */
    fun requiredValue(requirement: AchievementRequirement): Long = when (requirement) {
        is AchievementRequirement.CompletedWalks -> requirement.count.toLong()
        is AchievementRequirement.DistanceMeters -> requirement.meters
        is AchievementRequirement.TotalXp -> requirement.xp
        is AchievementRequirement.Level -> requirement.level.toLong()
        is AchievementRequirement.StreakDays -> requirement.days.toLong()
        is AchievementRequirement.Steps -> requirement.steps
        is AchievementRequirement.ActiveDurationSeconds -> requirement.seconds
    }

    fun isSatisfied(definition: AchievementDefinition, context: AchievementContext): Boolean =
        currentValue(definition.requirement, context) >= requiredValue(definition.requirement)

    /**
     * All achievement ids satisfied by [context], in definition order. Persisted
     * unlock state is intentionally not an input — the repository merges both, so
     * re-running this over the same context is always safe.
     */
    fun evaluate(
        context: AchievementContext,
        definitions: List<AchievementDefinition>,
    ): List<String> = definitions.filter { isSatisfied(it, context) }.map { it.id }

    /**
     * Progress fraction toward a locked achievement, for display only. Clamped to
     * 0..1; a zero or negative target reports full progress so no requirement can
     * render a nonsensical bar.
     */
    fun progressFraction(
        requirement: AchievementRequirement,
        context: AchievementContext,
    ): Float {
        val target = requiredValue(requirement)
        if (target <= 0L) return 1f
        val current = currentValue(requirement, context).coerceAtLeast(0L)
        return (current.toDouble() / target.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
