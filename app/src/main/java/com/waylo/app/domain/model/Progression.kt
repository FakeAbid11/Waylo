package com.waylo.app.domain.model

enum class AwardOutcome {
    Awarded,
    AlreadyAwarded,
    NotQualifying,
    NotPersisted,
}

data class ProgressionResult(
    val outcome: AwardOutcome,
    val activityId: Long,
    val xpAwarded: Int,
    val totalXp: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
) {
    val leveledUp: Boolean
        get() = levelAfter > levelBefore
}

data class ProgressionAwardEvent(
    val activityId: Long,
    val xpAwarded: Int,
    val totalXp: Int,
    val levelBefore: Int,
    val levelAfter: Int,
)
