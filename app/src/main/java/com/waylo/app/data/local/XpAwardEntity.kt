package com.waylo.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "xp_awards",
    indices = [Index(value = ["activityId"], unique = true)],
)
data class XpAwardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val activityId: Long,
    val xp: Int,
    val awardedAt: Long,
)
