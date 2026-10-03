package com.waylo.app.core.common

import com.waylo.app.domain.model.AchievementCategory
import com.waylo.app.domain.model.AchievementDefinition
import com.waylo.app.domain.model.AchievementRequirement

/**
 * The Phase 10 achievement catalog: immutable definitions living in code.
 *
 * Every requirement is computed from data Waylo already persists locally:
 * - distance / walks / steps / time  → aggregates over `walking_sessions`
 *   where `state = 'Completed'` (retroactive for existing history);
 * - XP / level                       → the Phase 8 `xp_awards` ledger;
 * - streak                           → longest streak derived from that ledger
 *   (so pre-Phase-8 days, which were never recorded, are simply unknown).
 *
 * Ids are stable, non-localized strings and must never change once shipped.
 * Rewards are recognition only — no definition grants XP.
 */
object AchievementCatalog {

    val definitions: List<AchievementDefinition> = listOf(
        // Walks ----------------------------------------------------------------
        AchievementDefinition(
            id = "first_walk",
            title = "First Walk",
            description = "Complete your first walk.",
            category = AchievementCategory.WALKS,
            requirement = AchievementRequirement.CompletedWalks(count = 1),
        ),
        AchievementDefinition(
            id = "walks_10",
            title = "10 Walks",
            description = "Complete 10 walks.",
            category = AchievementCategory.WALKS,
            requirement = AchievementRequirement.CompletedWalks(count = 10),
        ),
        AchievementDefinition(
            id = "walks_50",
            title = "50 Walks",
            description = "Complete 50 walks.",
            category = AchievementCategory.WALKS,
            requirement = AchievementRequirement.CompletedWalks(count = 50),
        ),
        AchievementDefinition(
            id = "walks_100",
            title = "100 Walks",
            description = "Complete 100 walks.",
            category = AchievementCategory.WALKS,
            requirement = AchievementRequirement.CompletedWalks(count = 100),
        ),

        // Distance (lifetime, completed walks only) -----------------------------
        AchievementDefinition(
            id = "distance_1km",
            title = "Walk 1 km",
            description = "Walk a total of 1 kilometer.",
            category = AchievementCategory.DISTANCE,
            requirement = AchievementRequirement.DistanceMeters(meters = 1_000L),
        ),
        AchievementDefinition(
            id = "distance_5km",
            title = "Walk 5 km",
            description = "Walk a total of 5 kilometers.",
            category = AchievementCategory.DISTANCE,
            requirement = AchievementRequirement.DistanceMeters(meters = 5_000L),
        ),
        AchievementDefinition(
            id = "distance_10km",
            title = "Walk 10 km",
            description = "Walk a total of 10 kilometers.",
            category = AchievementCategory.DISTANCE,
            requirement = AchievementRequirement.DistanceMeters(meters = 10_000L),
        ),
        AchievementDefinition(
            id = "distance_50km",
            title = "Walk 50 km",
            description = "Walk a total of 50 kilometers.",
            category = AchievementCategory.DISTANCE,
            requirement = AchievementRequirement.DistanceMeters(meters = 50_000L),
        ),
        AchievementDefinition(
            id = "distance_100km",
            title = "Walk 100 km",
            description = "Walk a total of 100 kilometers.",
            category = AchievementCategory.DISTANCE,
            requirement = AchievementRequirement.DistanceMeters(meters = 100_000L),
        ),

        // Steps (recorded walk steps, completed walks only) ----------------------
        AchievementDefinition(
            id = "steps_1000",
            title = "1,000 Steps",
            description = "Walk a total of 1,000 steps.",
            category = AchievementCategory.STEPS,
            requirement = AchievementRequirement.Steps(steps = 1_000L),
        ),
        AchievementDefinition(
            id = "steps_10000",
            title = "10,000 Steps",
            description = "Walk a total of 10,000 steps.",
            category = AchievementCategory.STEPS,
            requirement = AchievementRequirement.Steps(steps = 10_000L),
        ),
        AchievementDefinition(
            id = "steps_50000",
            title = "50,000 Steps",
            description = "Walk a total of 50,000 steps.",
            category = AchievementCategory.STEPS,
            requirement = AchievementRequirement.Steps(steps = 50_000L),
        ),
        AchievementDefinition(
            id = "steps_100000",
            title = "100,000 Steps",
            description = "Walk a total of 100,000 steps.",
            category = AchievementCategory.STEPS,
            requirement = AchievementRequirement.Steps(steps = 100_000L),
        ),
        AchievementDefinition(
            id = "steps_1000000",
            title = "1,000,000 Steps",
            description = "Walk a total of 1,000,000 steps.",
            category = AchievementCategory.STEPS,
            requirement = AchievementRequirement.Steps(steps = 1_000_000L),
        ),

        // Streaks (longest streak ever reached) ---------------------------------
        AchievementDefinition(
            id = "streak_3",
            title = "3 Day Streak",
            description = "Build a streak of 3 consecutive days.",
            category = AchievementCategory.STREAK,
            requirement = AchievementRequirement.StreakDays(days = 3),
        ),
        AchievementDefinition(
            id = "streak_7",
            title = "7 Day Streak",
            description = "Build a streak of 7 consecutive days.",
            category = AchievementCategory.STREAK,
            requirement = AchievementRequirement.StreakDays(days = 7),
        ),
        AchievementDefinition(
            id = "streak_14",
            title = "14 Day Streak",
            description = "Build a streak of 14 consecutive days.",
            category = AchievementCategory.STREAK,
            requirement = AchievementRequirement.StreakDays(days = 14),
        ),
        AchievementDefinition(
            id = "streak_30",
            title = "30 Day Streak",
            description = "Build a streak of 30 consecutive days.",
            category = AchievementCategory.STREAK,
            requirement = AchievementRequirement.StreakDays(days = 30),
        ),
        AchievementDefinition(
            id = "streak_100",
            title = "100 Day Streak",
            description = "Build a streak of 100 consecutive days.",
            category = AchievementCategory.STREAK,
            requirement = AchievementRequirement.StreakDays(days = 100),
        ),

        // XP (Phase 8 ledger total) ---------------------------------------------
        AchievementDefinition(
            id = "xp_1000",
            title = "1,000 XP",
            description = "Earn 1,000 total XP.",
            category = AchievementCategory.XP,
            requirement = AchievementRequirement.TotalXp(xp = 1_000L),
        ),
        AchievementDefinition(
            id = "xp_5000",
            title = "5,000 XP",
            description = "Earn 5,000 total XP.",
            category = AchievementCategory.XP,
            requirement = AchievementRequirement.TotalXp(xp = 5_000L),
        ),

        // Level (derived from the Phase 8 ledger) --------------------------------
        AchievementDefinition(
            id = "level_5",
            title = "Level 5",
            description = "Reach level 5.",
            category = AchievementCategory.LEVEL,
            requirement = AchievementRequirement.Level(level = 5),
        ),
        AchievementDefinition(
            id = "level_10",
            title = "Level 10",
            description = "Reach level 10.",
            category = AchievementCategory.LEVEL,
            requirement = AchievementRequirement.Level(level = 10),
        ),
    )

    private val byId: Map<String, AchievementDefinition> = definitions.associateBy { it.id }

    fun definition(id: String): AchievementDefinition? = byId[id]

    fun titleFor(id: String): String? = byId[id]?.title
}
