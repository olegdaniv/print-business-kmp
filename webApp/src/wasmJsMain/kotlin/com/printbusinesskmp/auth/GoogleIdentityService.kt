package com.printbusinesskmp.auth

import com.printbusinesskmp.shared.webGoogleClientIdFromBuildConfig

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    "(clientId, callback) => {" +
        "const g = window.google;" +
        "if (!g || !g.accounts || !g.accounts.id) return false;" +
        "g.accounts.id.initialize({" +
        "client_id: clientId," +
        "callback: (response) => {" +
        "const credential = response && response.credential ? response.credential : '';" +
        "callback(credential);" +
        "}" +
        "});" +
        "return true;" +
        "}"
)
private external fun gisInitialize(clientId: String, callback: (String) -> Unit): Boolean

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    "() => {" +
        "const g = window.google;" +
        "if (!g || !g.accounts || !g.accounts.id) return false;" +
        "g.accounts.id.prompt();" +
        "return true;" +
        "}"
)
private external fun gisPrompt(): Boolean

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => window.__PRINTBUSINESS_GOOGLE_CLIENT_ID || null")
private external fun readGoogleClientIdFromWindow(): String?

internal object GoogleIdentityService {
    private var initialized = false

    fun initialize(clientId: String, onCredential: (String) -> Unit): Boolean {
        val normalizedClientId = clientId.trim()
        if (normalizedClientId.isEmpty()) return false
        val initializedNow = gisInitialize(normalizedClientId) { credential ->
            if (credential.isNotBlank()) {
                onCredential(credential)
            }
        }
        initialized = initializedNow
        return initializedNow
    }

    fun promptSignIn(): Boolean {
        if (!initialized) return false
        return gisPrompt()
    }

    fun readClientId(): String? {
        return readGoogleClientIdFromWindow()?.trim()?.takeIf { it.isNotEmpty() }
            ?: webGoogleClientIdFromBuildConfig().trim().takeIf { it.isNotEmpty() }
    }
}
