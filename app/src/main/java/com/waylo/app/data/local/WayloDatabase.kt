package com.waylo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [], version = 1, exportSchema = false)
abstract class WayloDatabase : RoomDatabase() {

    companion object {
        private const val DATABASE_NAME = "waylo.db"

        @Volatile
        private var instance: WayloDatabase? = null

        fun getInstance(context: Context): WayloDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): WayloDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WayloDatabase::class.java,
                DATABASE_NAME,
            ).build()
        }
    }
}
