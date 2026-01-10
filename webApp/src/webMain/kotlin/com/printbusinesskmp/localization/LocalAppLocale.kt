package com.printbusinesskmp.localization

import androidx.compose.runtime.*
import kotlinx.browser.window

/**
 * Provides a custom locale for the web application.
 * This integrates with the navigator.languages override in index.html
 * to enable dynamic locale switching.
 */
object LocalAppLocale {
    private val LocalAppLocale = staticCompositionLocalOf { Locale.current }

    val current: String
        @Composable get() = LocalAppLocale.current.toString()

    @Composable
    infix fun provides(value: String?): ProvidedValue<*> {
        // Set the custom locale on the window object
        // The HTML script intercepts navigator.languages to return this value
        setCustomLocale(value?.replace('_', '-'))
        return LocalAppLocale.provides(Locale.current)
    }
}

/**
 * Represents the current browser locale
 */
object Locale {
    val current: String
        get() {
            val languages = window.navigator.languages
            return if (getLanguagesLength() > 0) {
                getFirstLanguage()
            } else {
                window.navigator.language
            }
        }

    override fun toString(): String = current
}

/**
 * Platform-specific interop functions for accessing navigator.languages
 */
internal expect fun getLanguagesLength(): Int
internal expect fun getFirstLanguage(): String

/**
 * Sets the custom locale on the window object using platform-specific interop
 */
internal expect fun setCustomLocale(locale: String?)