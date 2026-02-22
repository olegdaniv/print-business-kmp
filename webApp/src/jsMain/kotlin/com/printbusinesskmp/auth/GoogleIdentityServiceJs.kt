package com.printbusinesskmp.auth

import com.printbusinesskmp.shared.webGoogleClientIdFromBuildConfig

@Suppress("UnsafeCastFromDynamic")
internal actual object GoogleIdentityService {
    private var initializedClientId: String? = null

    actual fun initialize(clientId: String, onCredential: (String) -> Unit): Boolean {
        val normalizedClientId = clientId.trim()
        if (normalizedClientId.isEmpty()) return false

        val google = js("window.google")
        val identityApi = google?.accounts?.id ?: return false

        val configuration = js("{}")
        configuration.client_id = normalizedClientId
        configuration.callback = { response: dynamic ->
            val credential = response?.credential as? String
            if (!credential.isNullOrBlank()) {
                onCredential(credential)
            }
        }

        identityApi.initialize(configuration)
        initializedClientId = normalizedClientId
        return true
    }

    actual fun promptSignIn(): Boolean {
        if (initializedClientId.isNullOrBlank()) return false
        val google = js("window.google")
        val identityApi = google?.accounts?.id ?: return false
        identityApi.prompt()
        return true
    }

    actual fun readClientId(): String? {
        val value = js("window.__PRINTBUSINESS_GOOGLE_CLIENT_ID") as String?
        return value?.trim()?.takeIf { it.isNotEmpty() }
            ?: webGoogleClientIdFromBuildConfig().trim().takeIf { it.isNotEmpty() }
    }
}
