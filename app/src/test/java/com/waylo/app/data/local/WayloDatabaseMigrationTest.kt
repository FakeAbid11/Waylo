package com.waylo.app.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.waylo.app.domain.model.WalkingState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WayloDatabaseMigrationTest {

    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "waylo_migration_test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To2KeepsSettingsAndCreatesWalkTables() {
        createVersion1Database()

        val database = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()

        try {
            runBlocking {
                assertEquals("dark", database.settingsDao().getValue("appearance"))

                val id = database.walkingDao().insertSession(
                    WalkingSessionEntity(
                        state = WalkingState.Active.name,
                        startMillis = 1_000L,
                        updatedMillis = 2_000L,
                        distanceMeters = 12.5,
                        activeMillis = 3_000L,
                        pausedMillis = 0L,
                        activeSegmentStartMillis = 1_000L,
                        pausedSegmentStartMillis = null,
                        startLatitude = 52.0,
                        startLongitude = 13.0,
                        lastLatitude = 52.001,
                        lastLongitude = 13.0,
                        errorMessage = null,
                    ),
                )
                assertTrue(id > 0L)

                val restored = database.walkingDao().sessionById(id)
                assertNotNull(restored)
                assertEquals(WalkingState.Active.name, restored!!.state)
                assertEquals(12.5, restored.distanceMeters, 0.0)

                database.walkingDao().insertLocationPoint(
                    WalkingLocationPointEntity(
                        sessionId = id,
                        sequence = 0,
                        latitude = 52.0,
                        longitude = 13.0,
                        timestampMillis = 1_500L,
                        accuracyMeters = 4f,
                        altitudeMeters = null,
                        speedMps = null,
                    ),
                )
                val points = database.walkingDao().locationPoints(id)
                assertEquals(1, points.size)
                assertEquals(52.0, points[0].latitude, 0.0)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationIsIdempotentWhenTablesAlreadyExist() {
        createVersion1Database()

        val first = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()
        first.close()

        val second = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()

        try {
            runBlocking {
                assertEquals("dark", second.settingsDao().getValue("appearance"))
            }
        } finally {
            second.close()
        }
    }

    @Test
    fun migrationFrom2To3AddsWalkStepColumnsAndKeepsExistingData() {
        createVersion2Database()

        val database = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()

        try {
            runBlocking {
                assertEquals("dark", database.settingsDao().getValue("appearance"))

                val existing = database.walkingDao().latestSession()
                assertNotNull(existing)
                assertEquals(WalkingState.Active.name, existing!!.state)
                assertEquals(12.5, existing.distanceMeters, 0.0)
                assertNull(existing.walkStartStepCount)
                assertNull(existing.walkStepCount)

                val id = database.walkingDao().insertSession(
                    WalkingSessionEntity(
                        state = WalkingState.Completed.name,
                        startMillis = 3_000L,
                        updatedMillis = 4_000L,
                        distanceMeters = 800.0,
                        activeMillis = 600_000L,
                        pausedMillis = 0L,
                        activeSegmentStartMillis = null,
                        pausedSegmentStartMillis = null,
                        startLatitude = 52.0,
                        startLongitude = 13.0,
                        lastLatitude = 52.005,
                        lastLongitude = 13.0,
                        errorMessage = null,
                        walkStartStepCount = 9_000L,
                        walkStepCount = 1_500L,
                    ),
                )
                assertTrue(id > 0L)

                val inserted = database.walkingDao().sessionById(id)
                assertNotNull(inserted)
                assertEquals(9_000L, inserted!!.walkStartStepCount)
                assertEquals(1_500L, inserted.walkStepCount)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFrom3To4AddsProgressionWithoutAwardingHistoricalWalks() {
        createVersion3Database()

        val database = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()

        try {
            runBlocking {
                assertEquals("dark", database.settingsDao().getValue("appearance"))

                val history = database.walkingDao().observeCompletedSessions().first()
                assertEquals(1, history.size)
                assertEquals(1_500.0, history[0].distanceMeters, 0.0)
                assertEquals(9_000L, history[0].walkStartStepCount)
                assertEquals(2_000L, history[0].walkStepCount)

                assertEquals(0, database.progressionDao().awardCount())
                assertEquals(0, database.progressionDao().totalXp())

                val progression = database.progressionDao()
                    .progressionById(ProgressionEntity.SINGLE_ROW_ID)
                assertNotNull(progression)
                assertEquals(0, progression!!.totalXp)
                assertEquals(0, progression.currentStreakDays)
                assertEquals(0, progression.longestStreakDays)
                assertTrue(progression.createdAtMillis > 0L)

                val totals = database.progressionDao().completedTotals()
                assertEquals(2_000L, totals.steps)
                assertEquals(1_500.0, totals.distanceMeters, 0.001)
                assertEquals(900_000L, totals.activeMillis)

                val awarded = database.progressionDao().insertAward(
                    XpAwardEntity(activityId = history[0].id, xp = 150, awardedAt = 1L),
                )
                assertTrue(awarded > 0L)
                val conflicted = database.progressionDao().insertAward(
                    XpAwardEntity(activityId = history[0].id, xp = 999, awardedAt = 2L),
                )
                assertEquals(-1L, conflicted)
                assertEquals(1, database.progressionDao().awardCount())
                assertEquals(150, database.progressionDao().totalXp())
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion2Database() {
        createVersion1Database()

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    WayloDatabase.MIGRATION_1_2.migrate(db)
                }

                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        try {
            db.execSQL(
                "INSERT INTO `walking_sessions` (" +
                    "`state`, `startMillis`, `updatedMillis`, `distanceMeters`, " +
                    "`activeMillis`, `pausedMillis`, `activeSegmentStartMillis`, " +
                    "`pausedSegmentStartMillis`, `startLatitude`, `startLongitude`, " +
                    "`lastLatitude`, `lastLongitude`, `errorMessage`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(
                    WalkingState.Active.name,
                    1_000L,
                    2_000L,
                    12.5,
                    3_000L,
                    0L,
                    1_000L,
                    0L,
                    52.0,
                    13.0,
                    52.001,
                    13.0,
                    "",
                ),
            )
        } finally {
            db.close()
            helper.close()
        }
    }

    @Test
    fun migrationFrom4To5AddsAchievementsWithoutTouchingExistingData() {
        createVersion4Database()

        val database = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(
            WayloDatabase.MIGRATION_1_2,
            WayloDatabase.MIGRATION_2_3,
            WayloDatabase.MIGRATION_3_4,
            WayloDatabase.MIGRATION_4_5,
        ).build()

        try {
            runBlocking {
                // Existing data survives the non-destructive migration.
                assertEquals("dark", database.settingsDao().getValue("appearance"))

                val history = database.walkingDao().observeCompletedSessions().first()
                assertEquals(1, history.size)
                assertEquals(1_500.0, history[0].distanceMeters, 0.0)
                assertEquals(2_000L, history[0].walkStepCount)

                assertEquals(150, database.progressionDao().totalXp())
                val progression = database.progressionDao()
                    .progressionById(ProgressionEntity.SINGLE_ROW_ID)
                assertNotNull(progression)

                // Existing users start with every achievement locked — unlock rows
                // are only created by explicit evaluation, never by migration.
                assertEquals(0, database.achievementDao().unlockCount())
                assertTrue(database.achievementDao().observeUnlocks().first().isEmpty())

                // The new table is fully usable: insert-if-absent semantics hold.
                val inserted = database.achievementDao().insertUnlock(
                    AchievementEntity("first_walk", 9_000L),
                )
                assertTrue(inserted > 0L)
                val conflicted = database.achievementDao().insertUnlock(
                    AchievementEntity("first_walk", 9_999L),
                )
                assertEquals(-1L, conflicted)
                assertEquals(1, database.achievementDao().unlockCount())
                assertEquals(
                    9_000L,
                    database.achievementDao().unlockFor("first_walk")!!.unlockedAtMillis,
                )
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion4Database() {
        createVersion3Database()

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    WayloDatabase.MIGRATION_3_4.migrate(db)
                }

                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        try {
            // A Phase 8 XP award from before Phase 10 must survive the migration.
            db.execSQL(
                "INSERT INTO `xp_awards` (`activityId`, `xp`, `awardedAt`) VALUES (?, ?, ?)",
                arrayOf<Any>(42L, 150, 5_000L),
            )
        } finally {
            db.close()
            helper.close()
        }
    }

    private fun createVersion3Database() {
        createVersion2Database()

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    WayloDatabase.MIGRATION_2_3.migrate(db)
                }

                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        try {
            db.execSQL(
                "INSERT INTO `walking_sessions` (" +
                    "`state`, `startMillis`, `updatedMillis`, `distanceMeters`, " +
                    "`activeMillis`, `pausedMillis`, `activeSegmentStartMillis`, " +
                    "`pausedSegmentStartMillis`, `startLatitude`, `startLongitude`, " +
                    "`lastLatitude`, `lastLongitude`, `errorMessage`, " +
                    "`walkStartStepCount`, `walkStepCount`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(
                    WalkingState.Completed.name,
                    5_000L,
                    6_000L,
                    1_500.0,
                    900_000L,
                    0L,
                    0L,
                    0L,
                    52.0,
                    13.0,
                    52.02,
                    13.02,
                    "",
                    9_000L,
                    2_000L,
                ),
            )
        } finally {
            db.close()
            helper.close()
        }
    }

    private fun createVersion1Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `settings` (" +
                            "`key` TEXT NOT NULL, " +
                            "`value` TEXT NOT NULL, " +
                            "PRIMARY KEY(`key`))",
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        try {
            db.execSQL(
                "INSERT OR REPLACE INTO `settings` (`key`, `value`) VALUES (?, ?)",
                arrayOf<Any>("appearance", "dark"),
            )
        } finally {
            db.close()
            helper.close()
        }
    }
}
