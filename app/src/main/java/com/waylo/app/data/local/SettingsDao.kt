package com.waylo.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings ORDER BY `key`")
    fun observeAll(): Flow<List<SettingsEntity>>

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(setting: SettingsEntity)
}
