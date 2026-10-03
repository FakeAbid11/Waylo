package com.waylo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted achievement unlock. Only the *fact* of an unlock is stored —
 * definitions and live progress are derived from code + existing app data.
 *
 * The primary key makes unlocking idempotent: a repeated evaluation inserts
 * nothing and grants nothing twice. Unlocks are permanent: deleting a walk
 * never removes a row the user already earned.
 */
@Entity(tableName = "achievement_unlocks")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAtMillis: Long,
)
