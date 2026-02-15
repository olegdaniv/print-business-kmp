package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.BusinessProfilesTable
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.InvoiceLinesTable
import com.printbusinesskmp.database.tables.InvoicesTable
import com.printbusinesskmp.database.tables.LayoutsTable
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.database.tables.OutsourceJobsTable
import com.printbusinesskmp.database.tables.PartnersTable
import com.printbusinesskmp.platform.AppDataPaths
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

object DatabaseFactory {

    fun init() {
        val database = if (isDockerEnvironment()) {
            Database.connect(createPostgresDataSource())
        } else {
            val localConfig = createLocalH2Config()
            localConfig.dbBasePath?.let { basePath ->
                LocalH2BackupManager.createBackupIfPresent(
                    dbBasePath = basePath,
                    backupDir = AppDataPaths.resolved.backupDir
                )
            }
            Database.connect(
                url = localConfig.url,
                driver = "org.h2.Driver",
                user = localConfig.user,
                password = localConfig.password
            )
        }

        transaction(database) {
            SchemaVersionGuard.ensureCompatible()

            SchemaUtils.createMissingTablesAndColumns(
                ClientsTable,
                BusinessProfilesTable,
                OrdersTable,
                OrderItemsTable,
                PartnersTable,
                OutsourceJobsTable,
                InvoicesTable,
                InvoiceLinesTable,
                LayoutsTable
            )
        }
    }

    private fun isDockerEnvironment(): Boolean {
        return System.getenv("DB_HOST") != null
    }

    private data class LocalH2Config(
        val url: String,
        val user: String,
        val password: String,
        val dbBasePath: Path?
    )

    private fun createLocalH2Config(): LocalH2Config {
        val customUrl = System.getenv("H2_URL")?.trim()?.takeIf { it.isNotEmpty() }
        val user = System.getenv("H2_USER")?.trim()?.takeIf { it.isNotEmpty() } ?: "sa"
        val password = System.getenv("H2_PASSWORD") ?: ""

        if (customUrl != null) {
            return LocalH2Config(
                url = customUrl,
                user = user,
                password = password,
                dbBasePath = parseH2BasePath(customUrl)
            )
        }

        migrateLegacyLocalDataIfNeeded()

        val dbBasePath = AppDataPaths.resolved.dbDir.resolve("printbusiness")
        val dbPath = dbBasePath.toAbsolutePath().normalize().toString()

        return LocalH2Config(
            url = "jdbc:h2:file:$dbPath;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1",
            user = user,
            password = password,
            dbBasePath = dbBasePath
        )
    }

    private fun createPostgresDataSource(): HikariDataSource {
        val dbHost = System.getenv("DB_HOST") ?: "localhost"
        val dbPort = System.getenv("DB_PORT") ?: "5432"
        val dbName = System.getenv("DB_NAME") ?: "printbusiness_db"
        val dbUser = System.getenv("DB_USER") ?: "printbusiness"
        val dbPassword = System.getenv("DB_PASSWORD") ?: ""

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            username = dbUser
            password = dbPassword

            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30_000
            connectionTimeout = 20_000
            maxLifetime = 1_800_000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun parseH2BasePath(jdbcUrl: String): Path? {
        val prefix = "jdbc:h2:file:"
        if (!jdbcUrl.startsWith(prefix, ignoreCase = true)) {
            return null
        }

        val rawPath = jdbcUrl.substringAfter(prefix).substringBefore(';').trim()
        if (rawPath.isEmpty()) return null

        val path = if (rawPath == "~") {
            System.getProperty("user.home")
        } else if (rawPath.startsWith("~/")) {
            System.getProperty("user.home") + rawPath.removePrefix("~")
        } else {
            rawPath
        }

        return runCatching {
            Paths.get(path).toAbsolutePath().normalize()
        }.getOrNull()
    }

    private fun migrateLegacyLocalDataIfNeeded() {
        val newPaths = AppDataPaths.resolved
        val newDbFile = newPaths.dbDir.resolve("printbusiness.mv.db")

        val userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val legacyDbDir = if (userDir.name == "backend") {
            userDir.resolve("data")
        } else {
            userDir.resolve("backend").resolve("data")
        }
        val legacyInvoicesDir = userDir.resolve("invoices")

        if (!newDbFile.exists() && legacyDbDir.exists() && legacyDbDir.isDirectory()) {
            val copied = copyLegacyDbFilesIfPresent(legacyDbDir, newPaths.dbDir)
            if (copied > 0) {
                println("Migrated $copied legacy H2 file(s) from '$legacyDbDir' to '${newPaths.dbDir}'.")
            }
        }

        val newInvoicesEmpty = runCatching {
            Files.list(newPaths.invoiceDir).use { stream -> stream.findAny().isEmpty }
        }.getOrDefault(true)
        if (newInvoicesEmpty && legacyInvoicesDir.exists() && legacyInvoicesDir.isDirectory()) {
            val copied = copyDirectoryContents(legacyInvoicesDir, newPaths.invoiceDir)
            if (copied > 0) {
                println("Migrated $copied legacy invoice file(s) from '$legacyInvoicesDir' to '${newPaths.invoiceDir}'.")
            }
        }
    }

    private fun copyLegacyDbFilesIfPresent(fromDir: Path, toDir: Path): Int {
        Files.createDirectories(toDir)
        val candidates = Files.list(fromDir).use { stream ->
            stream
                .filter { path ->
                    Files.isRegularFile(path) &&
                        path.fileName.toString().startsWith("printbusiness")
                }
                .toList()
        }

        var copied = 0
        for (source in candidates) {
            val target = toDir.resolve(source.fileName.toString())
            if (Files.exists(target)) {
                continue
            }
            runCatching {
                Files.copy(source, target)
                copied += 1
            }.onFailure { error ->
                System.err.println("Warning: failed to migrate legacy DB file '$source': ${error.message}")
            }
        }
        return copied
    }

    private fun copyDirectoryContents(fromDir: Path, toDir: Path): Int {
        Files.createDirectories(toDir)
        var copied = 0

        Files.walk(fromDir).use { stream ->
            stream
                .filter { path -> Files.isRegularFile(path) }
                .forEach { source ->
                    val relative = fromDir.relativize(source)
                    val target = toDir.resolve(relative)
                    runCatching {
                        target.parent?.let { Files.createDirectories(it) }
                        if (!Files.exists(target)) {
                            Files.copy(source, target)
                            copied += 1
                        }
                    }.onFailure { error ->
                        System.err.println("Warning: failed to migrate legacy invoice '$source': ${error.message}")
                    }
                }
        }
        return copied
    }
}
