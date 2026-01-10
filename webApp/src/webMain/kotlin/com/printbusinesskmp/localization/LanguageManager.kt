package com.printbusinesskmp.localization

import androidx.compose.runtime.*
import kotlinx.browser.localStorage

/**
 * Manages application language state and persistence.
 * Integrates with LocalAppLocale for proper resource loading.
 */
object LanguageManager {
    private const val LANGUAGE_KEY = "app_language"

    // Get saved language or default to Ukrainian
    private fun getSavedLanguage(): Language {
        val savedCode = localStorage.getItem(LANGUAGE_KEY)
        return if (savedCode != null) {
            Language.fromCode(savedCode)
        } else {
            Language.UKRAINIAN
        }
    }

    // Mutable state for current language
    private val _currentLanguage = mutableStateOf(getSavedLanguage())
    val currentLanguage: State<Language> = _currentLanguage

    /**
     * Changes the application language.
     * This will trigger recomposition and reload all string resources.
     */
    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        localStorage.setItem(LANGUAGE_KEY, language.code)

        // Update window.__customLocale which is read by navigator.languages override
        // This is defined in index.html and intercepted by the LocalAppLocale system
        com.printbusinesskmp.localization.setCustomLocale(language.code.replace('_', '-'))
    }
}
