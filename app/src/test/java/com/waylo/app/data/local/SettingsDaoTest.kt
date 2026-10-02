package com.waylo.app.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsDaoTest {

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
    fun settingsStartEmpty() = runBlocking {
        val dao = database.settingsDao()

        assertEquals(emptyList<SettingsEntity>(), dao.observeAll().first())
        assertNull(dao.getValue("appearance"))
    }

    @Test
    fun upsertStoresAndReplacesAValue() = runBlocking {
        val dao = database.settingsDao()

        dao.upsert(SettingsEntity(key = "appearance", value = "dark"))
        dao.upsert(SettingsEntity(key = "appearance", value = "light"))

        assertEquals("light", dao.getValue("appearance"))
        assertEquals(
            listOf(SettingsEntity(key = "appearance", value = "light")),
            dao.observeAll().first(),
        )
    }
}
