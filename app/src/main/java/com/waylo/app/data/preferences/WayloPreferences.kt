package com.waylo.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WayloPreferences(
    private val dataStore: DataStore<Preferences>,
) {

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    /** Optional body weight in kilograms for calorie estimates; null until the user sets it. */
    val weightKg: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[KEY_WEIGHT_KG]
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setWeightKg(weightKg: Int?) {
        dataStore.edit { preferences ->
            if (weightKg == null) {
                preferences.remove(KEY_WEIGHT_KG)
            } else {
                preferences[KEY_WEIGHT_KG] = weightKg
            }
        }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_WEIGHT_KG = intPreferencesKey("weight_kg")
    }
}
