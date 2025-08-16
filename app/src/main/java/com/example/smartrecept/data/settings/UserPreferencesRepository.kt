// data/settings/UserPreferencesRepository.kt
package com.example.smartrecept.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkTheme = prefs[DARK_THEME_KEY] ?: false,
            fontScale = prefs[FONT_SCALE_KEY] ?: 1.0f,
            language = prefs[LANGUAGE_KEY] ?: "en",
            themeMode = prefs[THEME_MODE] ?: "system"
        )
    }

    suspend fun updateTheme(isDark: Boolean) {
        context.dataStore.edit { it[DARK_THEME_KEY] = isDark }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE_KEY] = scale }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
