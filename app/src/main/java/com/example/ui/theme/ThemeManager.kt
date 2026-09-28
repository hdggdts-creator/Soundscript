package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app theme mode (Light, Dark, System Default) using AppCompatDelegate
 * and persists the selection in SharedPreferences.
 */
object ThemeManager {

    private const val PREFS_NAME = "soundscript_theme_prefs"
    private const val KEY_THEME = "app_theme_selection"

    const val MODE_LIGHT = "Light Mode"
    const val MODE_DARK = "Dark Mode"
    const val MODE_SYSTEM = "System Default"

    val THEME_OPTIONS = listOf(MODE_LIGHT, MODE_DARK, MODE_SYSTEM)

    private val _currentTheme = MutableStateFlow(MODE_SYSTEM)
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes theme on app startup based on persisted user choice in SharedPreferences.
     */
    fun initTheme(context: Context) {
        val saved = getPrefs(context).getString(KEY_THEME, MODE_SYSTEM) ?: MODE_SYSTEM
        _currentTheme.value = saved
        applyAppCompatNightMode(saved)
    }

    /**
     * Retrieves the persisted theme preference.
     */
    fun getThemePreference(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    /**
     * Sets and persists the user's theme selection using SharedPreferences and AppCompatDelegate.setDefaultNightMode().
     */
    fun setThemePreference(context: Context, themeChoice: String) {
        getPrefs(context).edit().putString(KEY_THEME, themeChoice).apply()
        _currentTheme.value = themeChoice
        applyAppCompatNightMode(themeChoice)
    }

    private fun applyAppCompatNightMode(themeChoice: String) {
        val nightMode = when (themeChoice) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
