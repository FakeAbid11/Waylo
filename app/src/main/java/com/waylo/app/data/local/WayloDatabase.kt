package com.waylo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SettingsEntity::class, WalkingSessionEntity::class, WalkingLocationPointEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class WayloDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    abstract fun walkingDao(): WalkingDao

    companion object {
        private const val DATABASE_NAME = "waylo.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `walking_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`state` TEXT NOT NULL, " +
                        "`startMillis` INTEGER NOT NULL, " +
                        "`updatedMillis` INTEGER NOT NULL, " +
                        "`distanceMeters` REAL NOT NULL, " +
                        "`activeMillis` INTEGER NOT NULL, " +
                        "`pausedMillis` INTEGER NOT NULL, " +
                        "`activeSegmentStartMillis` INTEGER, " +
                        "`pausedSegmentStartMillis` INTEGER, " +
                        "`startLatitude` REAL, " +
                        "`startLongitude` REAL, " +
                        "`lastLatitude` REAL, " +
                        "`lastLongitude` REAL, " +
                        "`errorMessage` TEXT)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `walking_location_points` (" +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`sequence` INTEGER NOT NULL, " +
                        "`latitude` REAL NOT NULL, " +
                        "`longitude` REAL NOT NULL, " +
                        "`timestampMillis` INTEGER NOT NULL, " +
                        "`accuracyMeters` REAL, " +
                        "`altitudeMeters` REAL, " +
                        "`speedMps` REAL, " +
                        "PRIMARY KEY(`sessionId`, `sequence`))",
                )
            }
        }

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
            ).addMigrations(MIGRATION_1_2).build()
        }
    }
}
