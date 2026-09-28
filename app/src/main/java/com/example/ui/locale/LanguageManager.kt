package com.example.ui.locale

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages in-app language selection (Arabic, English, System Default)
 * using AndroidX AppCompatDelegate and persists the choice in SharedPreferences.
 */
object LanguageManager {

    private const val PREFS_NAME = "soundscript_language_prefs"
    private const val KEY_LANGUAGE = "app_language_selection"

    const val LANG_SYSTEM = "system"
    const val LANG_EN = "en"
    const val LANG_AR = "ar"

    val LANGUAGE_OPTIONS = listOf(LANG_SYSTEM, LANG_EN, LANG_AR)

    private val _currentLanguage = MutableStateFlow(LANG_SYSTEM)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes language configuration on app startup.
     */
    fun initLanguage(context: Context) {
        val saved = getPrefs(context).getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        _currentLanguage.value = saved
        applyLanguage(saved)
    }

    /**
     * Retrieves the persisted language code ("system", "en", or "ar").
     */
    fun getLanguagePreference(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    /**
     * Sets and persists the user's language selection, applies the locale to the app runtime,
     * and recreates the host activity if requested to refresh strings and layout direction (RTL/LTR).
     */
    fun setLanguagePreference(context: Context, langCode: String, recreateActivity: Boolean = true) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, langCode).apply()
        _currentLanguage.value = langCode
        applyLanguage(langCode)

        if (recreateActivity && context is Activity) {
            context.recreate()
        }
    }

    /**
     * Applies the selected locale via AppCompatDelegate.setApplicationLocales().
     */
    fun applyLanguage(langCode: String) {
        val appLocale = when (langCode) {
            LANG_EN -> {
                Locale.setDefault(Locale.ENGLISH)
                LocaleListCompat.forLanguageTags("en")
            }
            LANG_AR -> {
                Locale.setDefault(Locale("ar"))
                LocaleListCompat.forLanguageTags("ar")
            }
            else -> {
                LocaleListCompat.getEmptyLocaleList()
            }
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
