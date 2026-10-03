package com.waylo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class CompletedTotalsRow(
    val steps: Long,
    val distanceMeters: Double,
    val activeMillis: Long,
)

@Dao
interface ProgressionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAward(award: XpAwardEntity): Long

    @Query("SELECT * FROM xp_awards WHERE activityId = :activityId LIMIT 1")
    suspend fun awardForActivity(activityId: Long): XpAwardEntity?

    @Query("SELECT xp FROM xp_awards WHERE activityId = :activityId LIMIT 1")
    fun observeAwardXp(activityId: Long): Flow<Int?>

    @Query("SELECT awardedAt FROM xp_awards WHERE xp > 0 ORDER BY awardedAt")
    suspend fun qualifyingAwardMillis(): List<Long>

    @Query("SELECT awardedAt FROM xp_awards WHERE xp > 0 ORDER BY awardedAt")
    fun observeQualifyingAwardMillis(): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM xp_awards")
    suspend fun totalXp(): Int

    @Query("SELECT COUNT(*) FROM xp_awards")
    suspend fun awardCount(): Int

    @Query("DELETE FROM xp_awards WHERE activityId = :activityId")
    suspend fun deleteAwardForActivity(activityId: Long): Int

    @Query("SELECT * FROM progression WHERE id = :id")
    suspend fun progressionById(id: Int): ProgressionEntity?

    @Query("SELECT * FROM progression WHERE id = :id")
    fun observeProgressionById(id: Int): Flow<ProgressionEntity?>

    @Upsert
    suspend fun upsertProgression(progression: ProgressionEntity)

    @Query(
        "SELECT COALESCE(SUM(walkStepCount), 0) AS steps, " +
            "COALESCE(SUM(distanceMeters), 0) AS distanceMeters, " +
            "COALESCE(SUM(activeMillis), 0) AS activeMillis " +
            "FROM walking_sessions WHERE state = 'Completed'",
    )
    fun observeCompletedTotals(): Flow<CompletedTotalsRow>

    @Query(
        "SELECT COALESCE(SUM(walkStepCount), 0) AS steps, " +
            "COALESCE(SUM(distanceMeters), 0) AS distanceMeters, " +
            "COALESCE(SUM(activeMillis), 0) AS activeMillis " +
            "FROM walking_sessions WHERE state = 'Completed'",
    )
    suspend fun completedTotals(): CompletedTotalsRow

    @Query("SELECT COUNT(*) FROM walking_sessions WHERE state = 'Completed'")
    fun observeCompletedWalkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM walking_sessions WHERE state = 'Completed'")
    suspend fun completedWalkCount(): Int
}
