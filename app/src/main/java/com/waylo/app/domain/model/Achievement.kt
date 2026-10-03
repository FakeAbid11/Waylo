package com.waylo.app.domain.model

/**
 * Achievement categories used only to group and filter the catalog.
 * Kept deliberately small so the screen stays understandable.
 */
enum class AchievementCategory(val label: String) {
    WALKS("Walks"),
    DISTANCE("Distance"),
    STEPS("Steps"),
    STREAK("Streaks"),
    XP("XP"),
    LEVEL("Levels"),
}

/**
 * A small, closed set of requirement shapes. Every requirement is a milestone
 * compared with `current >= required` — never exact equality, because real
 * activity values rarely land exactly on a boundary.
 */
sealed interface AchievementRequirement {
    data class CompletedWalks(val count: Int) : AchievementRequirement

    data class DistanceMeters(val meters: Long) : AchievementRequirement

    data class TotalXp(val xp: Long) : AchievementRequirement

    data class Level(val level: Int) : AchievementRequirement

    data class StreakDays(val days: Int) : AchievementRequirement

    data class Steps(val steps: Long) : AchievementRequirement

    data class ActiveDurationSeconds(val seconds: Long) : AchievementRequirement
}

/**
 * Recognition-only reward. Waylo achievements never grant XP, so a reward can
 * never inflate the Phase 8 XP ledger or create an XP loop.
 */
data class AchievementReward(
    val label: String,
    val xpBonus: Long,
) {
    companion object {
        val BadgeAndCelebration = AchievementReward(
            label = "Waylo badge + fox celebration",
            xpBonus = 0L,
        )
    }
}

/**
 * Immutable achievement definition. Definitions live in code (never in database
 * rows) so a localized title change can never corrupt stored progress.
 *
 * [id] is a stable, non-localized identifier (`first_walk`, `distance_10km`) that
 * survives text changes and is what Room persists.
 */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val requirement: AchievementRequirement,
    val reward: AchievementReward = AchievementReward.BadgeAndCelebration,
)

/**
 * All facts an achievement evaluation needs, summarized once from existing
 * local data. Nothing here is estimated or fabricated.
 */
data class AchievementContext(
    val completedWalkCount: Int,
    val totalDistanceMeters: Long,
    val totalXp: Long,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalSteps: Long,
    val totalActiveSeconds: Long,
) {
    companion object {
        fun empty(): AchievementContext = AchievementContext(
            completedWalkCount = 0,
            totalDistanceMeters = 0L,
            totalXp = 0L,
            level = 1,
            currentStreakDays = 0,
            longestStreakDays = 0,
            totalSteps = 0L,
            totalActiveSeconds = 0L,
        )
    }
}

/**
 * Derived view of achievement state: current totals plus which achievements are
 * unlocked. Unlocks are the persisted truth; totals are re-derived from Room
 * aggregates so progress never needs a stored snapshot.
 */
data class AchievementSnapshot(
    val context: AchievementContext,
    val unlockedIds: Set<String>,
    val unlockedAtById: Map<String, Long>,
)

/** Compact count shown on Profile and Progress. */
data class AchievementSummary(
    val unlockedCount: Int,
    val totalCount: Int,
)

/** One unlock produced by an idempotent insert. */
data class AchievementUnlock(
    val id: String,
    val unlockedAtMillis: Long,
)

/**
 * One-shot event emitted only for *newly* unlocked achievements, after the
 * Room insert succeeded. [activityId] is the walk that triggered the evaluation,
 * or null when reconciliation ran at startup (process-death recovery).
 */
data class AchievementUnlockedEvent(
    val activityId: Long?,
    val unlocks: List<AchievementUnlock>,
    val unlockedAtMillis: Long,
)
