// data/settings/UserPreferences.kt
package com.example.smartrecept.data.settings

data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val fontScale: Float = 1.0f,
    val language: String = "ru",
    val themeMode: String = "system" // или "light", "dark"
)
