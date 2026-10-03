package com.waylo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progression")
data class ProgressionEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val totalXp: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val updatedAtMillis: Long,
    val createdAtMillis: Long,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
