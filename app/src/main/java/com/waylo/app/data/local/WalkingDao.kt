package com.waylo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface WalkingDao {

    @Query("SELECT * FROM walking_sessions ORDER BY id DESC LIMIT 1")
    suspend fun latestSession(): WalkingSessionEntity?

    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): WalkingSessionEntity?

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
}
