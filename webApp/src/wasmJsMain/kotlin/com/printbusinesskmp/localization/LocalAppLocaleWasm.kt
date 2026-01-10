package com.printbusinesskmp.localization

/**
 * Wasm-specific implementation of navigator.languages interop using @JsFun
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => window.navigator.languages.length")
internal actual external fun getLanguagesLength(): Int

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => window.navigator.languages[0]")
internal actual external fun getFirstLanguage(): String

/**
 * Sets the custom locale on the window object
 */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(locale) => window.__customLocale = locale")
internal actual external fun setCustomLocale(locale: String?)