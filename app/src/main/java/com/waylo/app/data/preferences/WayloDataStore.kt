package com.waylo.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.wayloDataStore: DataStore<Preferences> by preferencesDataStore(name = "waylo_preferences")
