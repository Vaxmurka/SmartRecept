// data/settings/LocaleUtils.kt
package com.example.smartrecept.data.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.*

object LocaleUtils {

    fun setAppLocale(context: Context, language: String): Context {
        return if (language == "system") {
            context
        } else {
            updateLocale(context, language)
        }
    }

    private fun updateLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            configuration.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        return context
    }

    @Composable
    fun getCurrentLocale(): Locale {
        val configuration = LocalConfiguration.current
        return configuration.locales[0]
    }

    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "system" to "Системная",
            "ru" to "Русский",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "it" to "Italiano",
            "zh" to "中文",
            "ja" to "日本語",
            "ko" to "한국어"
        )
    }
}