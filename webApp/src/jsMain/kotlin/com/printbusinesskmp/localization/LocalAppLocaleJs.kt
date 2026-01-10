package com.printbusinesskmp.localization

/**
 * JS-specific implementation of navigator.languages interop
 */
internal actual fun getLanguagesLength(): Int {
    return js("window.navigator.languages.length") as Int
}

internal actual fun getFirstLanguage(): String {
    return js("window.navigator.languages[0]") as String
}

/**
 * Sets the custom locale on the window object
 */
internal actual fun setCustomLocale(locale: String?) {
    js("window.__customLocale = locale")
}