package com.printbusinesskmp.auth

internal actual fun sessionStorageGetItem(key: String): String? {
    return js("window.sessionStorage.getItem(key)") as String?
}

internal actual fun sessionStorageSetItem(key: String, value: String) {
    js("window.sessionStorage.setItem(key, value)")
}

internal actual fun sessionStorageRemoveItem(key: String) {
    js("window.sessionStorage.removeItem(key)")
}
