package com.example.brewlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val GRAMS_STEP_SIZE_KEY = floatPreferencesKey("grams_step_size")
    private val GRIND_STEP_SIZE_KEY = floatPreferencesKey("grind_step_size")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(themeName)
        }

    val gramsStepSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[GRAMS_STEP_SIZE_KEY] ?: 0.5f
        }

    val grindStepSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[GRIND_STEP_SIZE_KEY] ?: 0.5f
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun setGramsStepSize(stepSize: Float) {
        context.dataStore.edit { preferences ->
            preferences[GRAMS_STEP_SIZE_KEY] = stepSize
        }
    }

    suspend fun setGrindStepSize(stepSize: Float) {
        context.dataStore.edit { preferences ->
            preferences[GRIND_STEP_SIZE_KEY] = stepSize
        }
    }
}
