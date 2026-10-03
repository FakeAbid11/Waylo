package com.waylo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SettingsEntity::class,
        WalkingSessionEntity::class,
        WalkingLocationPointEntity::class,
        XpAwardEntity::class,
        ProgressionEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class WayloDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    abstract fun walkingDao(): WalkingDao

    abstract fun progressionDao(): ProgressionDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `walking_sessions` ADD COLUMN `walkStartStepCount` INTEGER",
                )
                db.execSQL(
                    "ALTER TABLE `walking_sessions` ADD COLUMN `walkStepCount` INTEGER",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `xp_awards` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`activityId` INTEGER NOT NULL, " +
                        "`xp` INTEGER NOT NULL, " +
                        "`awardedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_awards_activityId` " +
                        "ON `xp_awards` (`activityId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `progression` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`totalXp` INTEGER NOT NULL, " +
                        "`currentStreakDays` INTEGER NOT NULL, " +
                        "`longestStreakDays` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `progression` (" +
                        "`id`, `totalXp`, `currentStreakDays`, `longestStreakDays`, " +
                        "`updatedAtMillis`, `createdAtMillis`) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        1,
                        0,
                        0,
                        0,
                        0L,
                        System.currentTimeMillis(),
                    ),
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        }
    }
}
