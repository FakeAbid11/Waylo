package com.waylo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    /**
     * Atomic insert-if-absent. Returns -1 when the achievement was already
     * unlocked, which is what lets callers emit unlock events only for rows
     * that were actually inserted for the first time.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(unlock: AchievementEntity): Long

    @Query("SELECT * FROM achievement_unlocks")
    fun observeUnlocks(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievement_unlocks WHERE achievementId = :achievementId LIMIT 1")
    suspend fun unlockFor(achievementId: String): AchievementEntity?

    @Query("SELECT COUNT(*) FROM achievement_unlocks")
    suspend fun unlockCount(): Int

    @Query("SELECT COUNT(*) FROM achievement_unlocks")
    fun observeUnlockCount(): Flow<Int>
}
