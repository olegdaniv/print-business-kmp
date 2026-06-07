package com.printbusinesskmp.desktop.platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Persisted desktop settings (JSON in the app data dir), following the same
 * pattern as [com.printbusinesskmp.auth.SessionStore].
 */
object AppSettingsStore {
    private val settingsFile get() = DesktopPaths.appDataDir.resolve("settings.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Serializable
    private data class PersistedSettings(
        val invoicesDir: String? = null
    )

    @Volatile
    private var cached: PersistedSettings? = null

    private fun load(): PersistedSettings {
        cached?.let { return it }
        val loaded = try {
            if (Files.exists(settingsFile)) {
                json.decodeFromString<PersistedSettings>(Files.readString(settingsFile))
            } else {
                PersistedSettings()
            }
        } catch (_: Exception) {
            PersistedSettings()
        }
        cached = loaded
        return loaded
    }

    private fun persist(settings: PersistedSettings) {
        cached = settings
        try {
            Files.writeString(settingsFile, json.encodeToString(settings))
        } catch (_: Exception) {
        }
    }

    /** Folder where invoice PDFs are stored. Defaults to the app's invoices dir. */
    var invoicesDir: Path
        get() {
            val stored = load().invoicesDir?.trim()?.takeIf { it.isNotEmpty() }
            val dir = stored?.let { Paths.get(it) } ?: DesktopPaths.invoiceDownloadsDir
            runCatching { Files.createDirectories(dir) }
            return dir
        }
        set(value) {
            val normalized = value.toAbsolutePath().normalize()
            runCatching { Files.createDirectories(normalized) }
            persist(load().copy(invoicesDir = normalized.toString()))
        }
}
