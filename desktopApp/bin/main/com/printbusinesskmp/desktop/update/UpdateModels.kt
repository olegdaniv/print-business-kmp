package com.printbusinesskmp.desktop.update

import java.nio.file.Path
import java.time.Instant

internal data class UpdateRelease(
    val version: String,
    val notes: String,
    val windowsUrl: String,
    val sha256: String?
)

data class UpdateUiState(
    val currentVersion: String = AppBuildConfig.VERSION,
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false,
    val releaseNotes: String = "",
    val lastCheckedAt: Instant? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val downloadedInstaller: Path? = null,
    val errorMessage: String? = null,
    val warningMessage: String? = null
) {
    val progressFraction: Float?
        get() {
            val total = totalBytes ?: return null
            if (total <= 0L) return null
            return (downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
}
