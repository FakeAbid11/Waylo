package com.waylo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkingDao {

    @Query("SELECT * FROM walking_sessions ORDER BY id DESC LIMIT 1")
    suspend fun latestSession(): WalkingSessionEntity?

    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): WalkingSessionEntity?

    @Query(
        "SELECT * FROM walking_sessions WHERE state = 'Completed' " +
            "ORDER BY startMillis DESC, id DESC",
    )
    fun observeCompletedSessions(): Flow<List<WalkingSessionEntity>>

    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WalkingSessionEntity?>

    @Query("DELETE FROM walking_sessions WHERE id = :id AND state = 'Completed'")
    suspend fun deleteCompletedSession(id: Long): Int

    @Query("DELETE FROM walking_location_points WHERE sessionId = :sessionId")
    suspend fun deleteLocationPointsForSession(sessionId: Long)

    @Insert
    suspend fun insertSession(session: WalkingSessionEntity): Long

    @Update
    suspend fun updateSession(session: WalkingSessionEntity)

    @Insert
    suspend fun insertLocationPoint(point: WalkingLocationPointEntity)

    @Query("SELECT * FROM walking_location_points WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun locationPoints(sessionId: Long): List<WalkingLocationPointEntity>

    @Query("SELECT COUNT(*) FROM walking_sessions")
    suspend fun sessionCount(): Int

    @Transaction
    suspend fun recordProgress(session: WalkingSessionEntity, point: WalkingLocationPointEntity?) {
        updateSession(session)
        if (point != null) {
            insertLocationPoint(point)
        }
    }

    @Transaction
    suspend fun deleteActivity(id: Long): Boolean {
        if (deleteCompletedSession(id) == 0) return false
        deleteLocationPointsForSession(id)
        return true
    }
}
