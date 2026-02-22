package com.printbusinesskmp.database

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

internal object LocalH2BackupManager {
    private val timestampFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)

    fun createBackupIfPresent(
        dbBasePath: Path,
        backupDir: Path,
        now: Instant = Instant.now(),
        keepLast: Int = 5
    ): Path? {
        val sourceFile = dbBasePath.resolveSibling("${dbBasePath.fileName}.mv.db")
        if (!sourceFile.exists() || !sourceFile.isRegularFile()) {
            return null
        }

        Files.createDirectories(backupDir)
        val backupName = "${sourceFile.name}.bak-${timestampFormat.format(now)}"
        val backupFile = backupDir.resolve(backupName)

        try {
            Files.copy(sourceFile, backupFile)
            pruneOldBackups(backupDir, sourceFile.name, keepLast)
            return backupFile
        } catch (error: IOException) {
            System.err.println("Warning: failed to create H2 backup '$backupFile': ${error.message}")
            return null
        }
    }

    private fun pruneOldBackups(
        backupDir: Path,
        sourceName: String,
        keepLast: Int
    ) {
        val prefix = "$sourceName.bak-"
        val backupFiles = Files.list(backupDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.name.startsWith(prefix) }
                .sorted { left, right ->
                    try {
                        Files.getLastModifiedTime(right).compareTo(Files.getLastModifiedTime(left))
                    } catch (_: IOException) {
                        right.toString().compareTo(left.toString())
                    }
                }
                .toList()
        }

        backupFiles
            .drop(keepLast)
            .forEach { file ->
                runCatching {
                    Files.deleteIfExists(file)
                }.onFailure { error ->
                    System.err.println("Warning: failed to remove old backup '$file': ${error.message}")
                }
            }
    }
}
