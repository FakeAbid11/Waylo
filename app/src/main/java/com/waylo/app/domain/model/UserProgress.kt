package com.waylo.app.domain.model

import com.waylo.app.core.common.fractionToPercent
import com.waylo.app.core.common.ratioToFraction

data class UserProgress(
    val level: Int,
    val totalXp: Int,
    val xp: Int,
    val xpToNextLevel: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalSteps: Int,
    val totalDistanceKm: Double,
    val totalWalkingMinutes: Int,
) {
    val xpProgress: Float
        get() = ratioToFraction(xp.toFloat(), xpToNextLevel.toFloat())

    val xpProgressPercent: Int
        get() = fractionToPercent(xpProgress)

    companion object {
        const val STARTING_LEVEL = 1
        const val STARTING_XP_TO_NEXT_LEVEL = 100

        fun empty(): UserProgress {
            return UserProgress(
                level = STARTING_LEVEL,
                totalXp = 0,
                xp = 0,
                xpToNextLevel = STARTING_XP_TO_NEXT_LEVEL,
                currentStreakDays = 0,
                longestStreakDays = 0,
                totalSteps = 0,
                totalDistanceKm = 0.0,
                totalWalkingMinutes = 0,
            )
        }
    }
}
