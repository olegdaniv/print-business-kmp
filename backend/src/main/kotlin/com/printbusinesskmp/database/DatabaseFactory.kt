package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.BusinessProfilesTable
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.InvoiceLinesTable
import com.printbusinesskmp.database.tables.InvoicesTable
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.database.tables.OutsourceJobsTable
import com.printbusinesskmp.database.tables.PartnersTable
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
            Database.connect(createHikariDataSource())
        } else {
            Database.connect(
                url = "jdbc:h2:mem:printbusiness;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "root",
                password = ""
            )
        }

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                ClientsTable,
                BusinessProfilesTable,
                OrdersTable,
                OrderItemsTable,
                PartnersTable,
                OutsourceJobsTable,
                InvoicesTable,
                InvoiceLinesTable
            )
        }
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
}
