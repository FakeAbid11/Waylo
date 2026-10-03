package com.waylo.app.core.common

import com.waylo.app.core.util.WayloFormat

object MascotContent {

    fun message(decision: MascotDecision, context: MascotContext): String =
        when (decision.state) {
            MascotState.Idle -> "Make every walk an adventure."
            MascotState.Encouraging -> when (decision.reaction) {
                MascotReaction.WalkStarted -> "Let's go!"
                else -> "Ready when you are!"
            }

            MascotState.Walking -> "You're moving!"
            MascotState.Paused -> "Take your time."
            MascotState.Celebrating -> when (decision.reaction) {
                MascotReaction.AchievementUnlocked -> if (context.achievementCount > 1) {
                    "${WayloFormat.count(context.achievementCount)} achievements unlocked!"
                } else {
                    "Achievement unlocked!"
                }

                else -> "Walk complete!"
            }

            MascotState.XpEarned -> "Nice work! +${WayloFormat.count(context.xpAwarded)} XP"
            MascotState.LevelUp -> "Level up!"
            MascotState.Streak -> if (context.streakDays <= 1) {
                "Your streak is alive!"
            } else {
                "${WayloFormat.count(context.streakDays)} days strong!"
            }

            MascotState.Resting -> "Your next walk is waiting."
        }

    fun contentDescription(state: MascotState): String = when (state) {
        MascotState.Idle -> "Waylo fox, your walking companion"
        MascotState.Encouraging -> "Waylo fox encouraging you to walk"
        MascotState.Walking -> "Waylo fox showing your walk in progress"
        MascotState.Paused -> "Waylo fox showing your walk is paused"
        MascotState.Celebrating -> "Waylo fox celebrating your completed walk"
        MascotState.XpEarned -> "Waylo fox celebrating XP you earned"
        MascotState.LevelUp -> "Waylo fox celebrating a level up"
        MascotState.Streak -> "Waylo fox showing your current streak"
        MascotState.Resting -> "Waylo fox resting until your next walk"
    }
}
