package com.waylo.app.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.waylo.app.domain.model.WalkingState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        ).addMigrations(WayloDatabase.MIGRATION_1_2).build()

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
        ).addMigrations(WayloDatabase.MIGRATION_1_2).build()
        first.close()

        val second = Room.databaseBuilder(
            context,
            WayloDatabase::class.java,
            databaseName,
        ).addMigrations(WayloDatabase.MIGRATION_1_2).build()

        try {
            runBlocking {
                assertEquals("dark", second.settingsDao().getValue("appearance"))
            }
        } finally {
            second.close()
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
