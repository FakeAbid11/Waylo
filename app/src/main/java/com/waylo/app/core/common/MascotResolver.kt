package com.waylo.app.core.common

import com.waylo.app.domain.model.UserProgress
import com.waylo.app.domain.model.WalkingState

enum class MascotState {
    Idle,
    Encouraging,
    Walking,
    Paused,
    Celebrating,
    XpEarned,
    LevelUp,
    Streak,
    Resting,
}

enum class MascotReaction {
    None,
    Ready,
    WalkStarted,
    Walking,
    WalkPaused,
    WalkInterrupted,
    WalkCompleted,
    XpEarned,
    LevelUp,
    AchievementUnlocked,
    StreakMilestone,
    NoRecentActivity,
}

data class MascotContext(
    val walkState: WalkingState? = null,
    val justCompleted: Boolean = false,
    val xpAwarded: Int = 0,
    val levelUp: Boolean = false,
    val achievementCount: Int = 0,
    val streakDays: Int = 0,
    val hasWalkHistory: Boolean = false,
    val hasNoActivity: Boolean = false,
) {
    /** True when at least one achievement unlocked in this event. */
    val achievementUnlocked: Boolean
        get() = achievementCount > 0

    companion object {
        fun fromProgress(
            progress: UserProgress,
            walkState: WalkingState? = null,
            justCompleted: Boolean = false,
            xpAwarded: Int = 0,
            levelUp: Boolean = false,
        ): MascotContext {
            val hasHistory = progress.totalDistanceKm > 0.0 || progress.totalXp > 0
            return MascotContext(
                walkState = walkState,
                justCompleted = justCompleted,
                xpAwarded = xpAwarded,
                levelUp = levelUp,
                streakDays = progress.currentStreakDays,
                hasWalkHistory = hasHistory,
                hasNoActivity = !hasHistory,
            )
        }
    }
}

data class MascotDecision(
    val state: MascotState,
    val reaction: MascotReaction,
)

object MascotResolver {

    fun resolve(context: MascotContext): MascotDecision = when {
        context.levelUp -> MascotDecision(MascotState.LevelUp, MascotReaction.LevelUp)
        context.achievementUnlocked ->
            MascotDecision(MascotState.Celebrating, MascotReaction.AchievementUnlocked)
        context.xpAwarded > 0 -> MascotDecision(MascotState.XpEarned, MascotReaction.XpEarned)
        context.justCompleted -> MascotDecision(MascotState.Celebrating, MascotReaction.WalkCompleted)
        context.walkState == WalkingState.Paused ->
            MascotDecision(MascotState.Paused, MascotReaction.WalkPaused)
        context.walkState == WalkingState.Starting ->
            MascotDecision(MascotState.Encouraging, MascotReaction.WalkStarted)
        context.walkState == WalkingState.Error ->
            MascotDecision(MascotState.Encouraging, MascotReaction.WalkInterrupted)
        context.walkState == WalkingState.Active || context.walkState == WalkingState.Stopping ->
            MascotDecision(MascotState.Walking, MascotReaction.Walking)
        context.streakDays >= 1 ->
            MascotDecision(MascotState.Streak, MascotReaction.StreakMilestone)
        context.hasWalkHistory ->
            MascotDecision(MascotState.Encouraging, MascotReaction.Ready)
        context.hasNoActivity ->
            MascotDecision(MascotState.Resting, MascotReaction.NoRecentActivity)
        else -> MascotDecision(MascotState.Idle, MascotReaction.None)
    }
}
