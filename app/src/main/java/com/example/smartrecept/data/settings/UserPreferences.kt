// data/settings/UserPreferences.kt
package com.example.smartrecept.data.settings

import com.example.smartrecept.ui.screens.FontSizeOption

data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val fontScale: Float = 1.0f,
    val font: String = FontSizeOption.M.label,
    val language: String = "system", // "system", "ru", "en", "es", etc.
    val themeMode: String = "system" // или "light", "dark"
)
