package com.printbusinesskmp.localization

import androidx.compose.runtime.*
import kotlinx.browser.localStorage
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

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

    // Function to change language
    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        localStorage.setItem(LANGUAGE_KEY, language.code)

        // Update the custom locale for Compose Multiplatform's stringResource()
        updateBrowserLocale(language.code)
    }

    // Update browser's custom locale
    @OptIn(ExperimentalWasmJsInterop::class)
    private fun updateBrowserLocale(languageCode: String) {
        js("window.__customLocale = [languageCode]")
    }
}
