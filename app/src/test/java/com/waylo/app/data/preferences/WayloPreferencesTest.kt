package com.waylo.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WayloPreferencesTest {

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun freshInstallIsNotCompleted() = runBlocking {
        val preferences = createPreferences()

        assertFalse(preferences.onboardingCompleted.first())
    }

    @Test
    fun completingOnboardingPersistsAcrossRestarts() = runBlocking {
        val file = preferencesFile()
        val firstLaunch = createPreferences(file)
        assertFalse(firstLaunch.onboardingCompleted.first())

        firstLaunch.setOnboardingCompleted()
        assertTrue(firstLaunch.onboardingCompleted.first())

        scopes.first().cancel()
        delay(500)

        val relaunch = createPreferences(file)
        assertTrue(relaunch.onboardingCompleted.first())
    }

    @Test
    fun weightIsOptionalRemovableAndPersisted() = runBlocking {
        val preferences = createPreferences()

        assertNull(preferences.weightKg.first())

        preferences.setWeightKg(72)
        assertEquals(72, preferences.weightKg.first())

        preferences.setWeightKg(null)
        assertNull(preferences.weightKg.first())
    }

    private fun preferencesFile(): File = File(
        RuntimeEnvironment.getApplication().filesDir,
        "waylo_test.preferences_pb",
    ).apply { delete() }

    private fun createPreferences(file: File = preferencesFile()): WayloPreferences {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes.add(scope)
        return WayloPreferences(
            PreferenceDataStoreFactory.create(scope = scope) { file },
        )
    }
}
