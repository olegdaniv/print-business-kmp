package com.printbusinesskmp.auth

import com.printbusinesskmp.api.AuthSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
data class PersistedAuthSession(
    val accessToken: String,
    val expiresAtEpochSeconds: Long,
    val email: String,
    val name: String? = null
)

object WebAuthStorage {
    private const val STORAGE_KEY = "printbusiness.auth.session"
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun load(): PersistedAuthSession? {
        val raw = sessionStorageGetItem(STORAGE_KEY) ?: return null
        val parsed = runCatching {
            json.decodeFromString<PersistedAuthSession>(raw)
        }.getOrNull() ?: return null

        if (parsed.expiresAtEpochSeconds <= Clock.System.now().epochSeconds) {
            clear()
            return null
        }
        return parsed
    }

    fun save(from: AuthSession) {
        val nowEpochSeconds = Clock.System.now().epochSeconds
        val persisted = PersistedAuthSession(
            accessToken = from.accessToken,
            expiresAtEpochSeconds = nowEpochSeconds + from.expiresInSeconds,
            email = from.email,
            name = from.name
        )
        sessionStorageSetItem(STORAGE_KEY, json.encodeToString(persisted))
    }

    fun clear() {
        sessionStorageRemoveItem(STORAGE_KEY)
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.getItem(key)")
private external fun sessionStorageGetItemJs(key: String): String?

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => window.sessionStorage.setItem(key, value)")
private external fun sessionStorageSetItemJs(key: String, value: String)

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.sessionStorage.removeItem(key)")
private external fun sessionStorageRemoveItemJs(key: String)

private fun sessionStorageGetItem(key: String): String? = sessionStorageGetItemJs(key)

private fun sessionStorageSetItem(key: String, value: String) {
    sessionStorageSetItemJs(key, value)
}

private fun sessionStorageRemoveItem(key: String) {
    sessionStorageRemoveItemJs(key)
}
