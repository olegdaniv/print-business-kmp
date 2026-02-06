package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.InvoiceItemsTable
import com.printbusinesskmp.database.tables.InvoicesTable
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {
        val database = if (isDockerEnvironment()) {
            // PostgreSQL configuration for Docker/Production
            Database.connect(createHikariDataSource())
        } else {
            // H2 in-memory database for local development
            Database.connect(
                url = "jdbc:h2:mem:printbusiness;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "root",
                password = ""
            )
        }

        transaction(database) {
            SchemaUtils.create(ClientsTable)
            SchemaUtils.create(OrdersTable)
            SchemaUtils.create(OrderItemsTable)
            SchemaUtils.create(InvoicesTable)
            SchemaUtils.create(InvoiceItemsTable)
        }

        println("Database initialized: ${if (isDockerEnvironment()) "PostgreSQL" else "H2 (in-memory)"}")
    }

    private fun isDockerEnvironment(): Boolean {
        return System.getenv("DB_HOST") != null
    }

    private fun createHikariDataSource(): HikariDataSource {
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

            // Connection pool settings
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000        // 30 seconds
            connectionTimeout = 20000  // 20 seconds
            maxLifetime = 1800000      // 30 minutes

            // Validation
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Additional recommended settings
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}