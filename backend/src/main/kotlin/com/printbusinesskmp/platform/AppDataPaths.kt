package com.printbusinesskmp.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object AppDataPaths {
    private const val APP_NAME = "PrintBusiness"
    private const val DATA_ROOT_ENV = "PRINTBUSINESS_DATA_ROOT"

    data class ResolvedPaths(
        val dataRoot: Path,
        val dbDir: Path,
        val invoiceDir: Path,
        val backupDir: Path
    )

    val resolved: ResolvedPaths by lazy {
        val root = resolveDataRoot(
            osName = System.getProperty("os.name"),
            homeDir = System.getProperty("user.home"),
            dataRootOverride = System.getenv(DATA_ROOT_ENV),
            localAppData = System.getenv("LOCALAPPDATA")
        )

        val dbDir = root.resolve("data")
        val invoiceDir = root.resolve("invoices")
        val backupDir = root.resolve("backups")

        Files.createDirectories(root)
        Files.createDirectories(dbDir)
        Files.createDirectories(invoiceDir)
        Files.createDirectories(backupDir)

        ResolvedPaths(
            dataRoot = root,
            dbDir = dbDir,
            invoiceDir = invoiceDir,
            backupDir = backupDir
        )
    }

    internal fun resolveDataRoot(
        osName: String,
        homeDir: String,
        dataRootOverride: String?,
        localAppData: String?
    ): Path {
        dataRootOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Paths.get(it).toAbsolutePath().normalize() }

        val os = osName.lowercase()
        val baseDir = when {
            os.contains("win") -> {
                val local = localAppData?.trim().orEmpty()
                if (local.isNotEmpty()) {
                    Paths.get(local)
                } else {
                    Paths.get(homeDir, "AppData", "Local")
                }
            }
            os.contains("mac") -> Paths.get(homeDir, "Library", "Application Support")
            else -> Paths.get(homeDir, ".local", "share")
        }

        return baseDir.resolve(APP_NAME).toAbsolutePath().normalize()
    }
}
