package com.example.brewlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.brewlog.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        fun isRealApiKey(key: String): Boolean {
            val k = key.trim()
            return k.isNotBlank() && k != "DEFAULT_KEY" && k != "none"
        }
    }

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val GRAMS_STEP_SIZE_KEY = floatPreferencesKey("grams_step_size")
    private val GRIND_STEP_SIZE_KEY = floatPreferencesKey("grind_step_size")
    private val GEMINI_API_KEY_KEY = stringPreferencesKey("gemini_api_key")
    private val AI_ENABLED_KEY = booleanPreferencesKey("ai_enabled")

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

    val geminiApiKey: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GEMINI_API_KEY_KEY] ?: ""
        }

    val aiEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AI_ENABLED_KEY] ?: true
        }

    val isApiKeyConfigured: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val customKey = preferences[GEMINI_API_KEY_KEY] ?: ""
            isRealApiKey(customKey) || isRealApiKey(BuildConfig.GEMINI_API_KEY)
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

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY_KEY] = apiKey.trim()
        }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AI_ENABLED_KEY] = enabled
        }
    }
}
