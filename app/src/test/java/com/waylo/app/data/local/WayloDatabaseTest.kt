package com.waylo.app.data.local

import androidx.room.Room
import com.waylo.app.WayloApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WayloDatabaseTest {

    @Test
    fun databaseInitializesAndOpens() {
        val database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            WayloDatabase::class.java,
        ).build()

        try {
            database.openHelper.readableDatabase
            assertTrue(database.isOpen)
        } finally {
            database.close()
        }
        assertFalse(database.isOpen)
    }

    @Test
    fun applicationProvidesASingleInitializedDatabase() {
        val application = RuntimeEnvironment.getApplication() as WayloApplication

        val database = application.wayloDatabase

        database.openHelper.readableDatabase
        assertTrue(database.isOpen)
        assertSame(database, application.wayloDatabase)
        database.close()
    }
}
