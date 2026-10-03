package com.waylo.app.data.local

import androidx.room.Room
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
class AchievementDaoTest {

    private lateinit var database: WayloDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertingAnUnlockReturnsARowIdAndStoresTheTimestamp() = runBlocking {
        val dao = database.achievementDao()

        val rowId = dao.insertUnlock(AchievementEntity("first_walk", 1_000L))

        assertTrue(rowId > 0L)
        val stored = dao.unlockFor("first_walk")
        assertNotNull(stored)
        assertEquals("first_walk", stored!!.achievementId)
        assertEquals(1_000L, stored.unlockedAtMillis)
    }

    @Test
    fun duplicateInsertsAreIgnoredAndKeepTheOriginalTimestamp() = runBlocking {
        val dao = database.achievementDao()

        val first = dao.insertUnlock(AchievementEntity("first_walk", 1_000L))
        val second = dao.insertUnlock(AchievementEntity("first_walk", 9_999L))

        assertTrue(first > 0L)
        assertEquals(-1L, second)
        assertEquals(1, dao.unlockCount())
        assertEquals(1_000L, dao.unlockFor("first_walk")!!.unlockedAtMillis)
    }

    @Test
    fun lookupByUnknownIdReturnsNull() = runBlocking {
        assertNull(database.achievementDao().unlockFor("does_not_exist"))
    }

    @Test
    fun countReportsEveryPersistedUnlock() = runBlocking {
        val dao = database.achievementDao()
        dao.insertUnlock(AchievementEntity("first_walk", 1L))
        dao.insertUnlock(AchievementEntity("distance_1km", 2L))
        dao.insertUnlock(AchievementEntity("distance_1km", 3L))

        assertEquals(2, dao.unlockCount())
        assertEquals(2, dao.observeUnlockCount().first())
    }

    @Test
    fun observeUnlocksEmitsEveryStoredRow() = runBlocking {
        val dao = database.achievementDao()
        assertEquals(0, dao.observeUnlocks().first().size)

        dao.insertUnlock(AchievementEntity("first_walk", 42L))
        dao.insertUnlock(AchievementEntity("streak_3", 43L))

        val rows = dao.observeUnlocks().first()
        assertEquals(2, rows.size)
        assertEquals(
            listOf("first_walk", "streak_3"),
            rows.map { it.achievementId }.sorted(),
        )
        assertEquals(42L, rows.first { it.achievementId == "first_walk" }.unlockedAtMillis)
    }
}
