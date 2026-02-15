package com.printbusinesskmp.database

import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalH2BackupManagerTest {

    @Test
    fun `creates backup when source h2 file exists`() {
        val tempRoot = createTempDirectory("h2-backup-test")
        val dbDir = tempRoot.resolve("data").createDirectories()
        val backupDir = tempRoot.resolve("backups")
        val dbBasePath = dbDir.resolve("printbusiness")
        val sourceDbFile = dbDir.resolve("printbusiness.mv.db")

        Files.writeString(sourceDbFile, "test-db-content")

        val backup = LocalH2BackupManager.createBackupIfPresent(
            dbBasePath = dbBasePath,
            backupDir = backupDir,
            now = Instant.parse("2026-01-01T00:00:00Z"),
            keepLast = 5
        )

        assertNotNull(backup)
        assertTrue(backup.exists())
        assertTrue(backup.isRegularFile())
        assertEquals("test-db-content", Files.readString(backup))
    }

    @Test
    fun `keeps only configured number of newest backups`() {
        val tempRoot = createTempDirectory("h2-backup-retention-test")
        val dbDir = tempRoot.resolve("data").createDirectories()
        val backupDir = tempRoot.resolve("backups")
        val dbBasePath = dbDir.resolve("printbusiness")
        val sourceDbFile = dbDir.resolve("printbusiness.mv.db")

        Files.writeString(sourceDbFile, "v1")

        val timestamps = listOf(
            "2026-01-01T00:00:00Z",
            "2026-01-01T00:00:01Z",
            "2026-01-01T00:00:02Z",
            "2026-01-01T00:00:03Z"
        ).map(Instant::parse)

        timestamps.forEachIndexed { index, instant ->
            Files.writeString(sourceDbFile, "content-$index")
            LocalH2BackupManager.createBackupIfPresent(
                dbBasePath = dbBasePath,
                backupDir = backupDir,
                now = instant,
                keepLast = 2
            )
        }

        val backups = Files.list(backupDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.name.startsWith("printbusiness.mv.db.bak-") }
                .toList()
        }

        assertEquals(2, backups.size)
    }
}
