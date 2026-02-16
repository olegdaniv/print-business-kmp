package com.printbusinesskmp.auth

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.getItem(key)")
private external fun sessionStorageGetItemJs(key: String): String?

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => window.sessionStorage.setItem(key, value)")
private external fun sessionStorageSetItemJs(key: String, value: String)

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.removeItem(key)")
private external fun sessionStorageRemoveItemJs(key: String)

internal actual fun sessionStorageGetItem(key: String): String? = sessionStorageGetItemJs(key)

internal actual fun sessionStorageSetItem(key: String, value: String) {
    sessionStorageSetItemJs(key, value)
}

internal actual fun sessionStorageRemoveItem(key: String) {
    sessionStorageRemoveItemJs(key)
}
