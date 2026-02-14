package com.printbusinesskmp.desktop.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object DesktopPaths {
    private const val APP_NAME = "PrintBusinessKmp"

    val appDataDir: Path by lazy {
        val osName = System.getProperty("os.name").lowercase()
        val homeDir = System.getProperty("user.home")

        val baseDir = when {
            osName.contains("mac") -> Paths.get(homeDir, "Library", "Application Support")
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrBlank()) {
                    Paths.get(appData)
                } else {
                    Paths.get(homeDir, "AppData", "Roaming")
                }
            }
            else -> Paths.get(homeDir, ".local", "share")
        }

        baseDir.resolve(APP_NAME).also { path ->
            Files.createDirectories(path)
        }
    }

    val invoiceDownloadsDir: Path by lazy {
        appDataDir.resolve("invoices").also { path ->
            Files.createDirectories(path)
        }
    }
}
