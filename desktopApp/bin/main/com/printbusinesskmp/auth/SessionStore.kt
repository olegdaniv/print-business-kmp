package com.printbusinesskmp.auth

import com.printbusinesskmp.api.AuthSession
import com.printbusinesskmp.desktop.platform.DesktopPaths
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files

object SessionStore {
    private val sessionFile get() = DesktopPaths.appDataDir.resolve("session.json")
    private val json = Json { ignoreUnknownKeys = true }
    private const val EXPIRY_BUFFER_MILLIS = 60_000L

    @Serializable
    private data class PersistedSession(
        val accessToken: String,
        val expiresAtMillis: Long,
        val email: String,
        val name: String? = null
    )

    fun save(session: AuthSession) {
        try {
            val expiresAtMillis = System.currentTimeMillis() + session.expiresInSeconds * 1_000L
            val persisted = PersistedSession(
                accessToken = session.accessToken,
                expiresAtMillis = expiresAtMillis,
                email = session.email,
                name = session.name
            )
            Files.writeString(sessionFile, json.encodeToString(persisted))
        } catch (_: Exception) {
        }
    }

    fun load(): AuthSession? {
        return try {
            if (!Files.exists(sessionFile)) return null
            val text = Files.readString(sessionFile)
            val persisted = json.decodeFromString<PersistedSession>(text)
            val remainingMillis = persisted.expiresAtMillis - System.currentTimeMillis()
            if (remainingMillis <= EXPIRY_BUFFER_MILLIS) {
                clear()
                return null
            }
            AuthSession(
                accessToken = persisted.accessToken,
                expiresInSeconds = remainingMillis / 1_000L,
                email = persisted.email,
                name = persisted.name
            )
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        try {
            Files.deleteIfExists(sessionFile)
        } catch (_: Exception) {
        }
    }
}