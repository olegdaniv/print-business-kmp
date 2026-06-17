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
        val invoicesDir: String? = null,
        val darkTheme: Boolean? = null,
        val deliveryNoteSeq: Int = 0,
        val deliveryNoteByInvoice: Map<String, String> = emptyMap()
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

    /** UI theme choice; persists across restarts. Defaults to light. */
    var isDarkTheme: Boolean
        get() = load().darkTheme ?: false
        set(value) {
            persist(load().copy(darkTheme = value))
        }

    /**
     * Returns the delivery-note number for an invoice, allocating the next number
     * in the local "ВН-" series on first request. Idempotent per invoice, so
     * regenerating keeps the same number.
     */
    /** Delivery-note number already assigned to an invoice, or null if none yet. */
    fun existingDeliveryNoteNumber(invoiceId: String): String? =
        load().deliveryNoteByInvoice[invoiceId]

    @Synchronized
    fun deliveryNoteNumber(invoiceId: String): String {
        val current = load()
        current.deliveryNoteByInvoice[invoiceId]?.let { return it }
        val nextSeq = current.deliveryNoteSeq + 1
        val number = "ВН-" + nextSeq.toString().padStart(4, '0')
        persist(
            current.copy(
                deliveryNoteSeq = nextSeq,
                deliveryNoteByInvoice = current.deliveryNoteByInvoice + (invoiceId to number)
            )
        )
        return number
    }
}
