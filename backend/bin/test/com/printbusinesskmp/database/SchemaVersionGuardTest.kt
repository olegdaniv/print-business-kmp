package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.SchemaVersionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchemaVersionGuardTest {

    @Test
    fun `creates schema version row for new database`() {
        connectFreshInMemoryDatabase("schema_guard_new")

        transaction {
            SchemaVersionGuard.ensureCompatible()

            val row = SchemaVersionTable.selectAll().singleOrNull()
            requireNotNull(row)
            assertEquals(SchemaVersionGuard.EXPECTED_SCHEMA_VERSION, row[SchemaVersionTable.version])
        }
    }

    @Test
    fun `throws clear error when schema version mismatches`() {
        connectFreshInMemoryDatabase("schema_guard_mismatch")

        transaction {
            SchemaUtils.create(SchemaVersionTable)
            SchemaVersionTable.insert {
                it[id] = "main"
                it[version] = SchemaVersionGuard.EXPECTED_SCHEMA_VERSION + 1
                it[updatedAt] = Instant.now()
            }
        }

        val error = assertFailsWith<IllegalStateException> {
            transaction {
                SchemaVersionGuard.ensureCompatible()
            }
        }

        assertContains(error.message.orEmpty(), "Database schema version")
        assertContains(error.message.orEmpty(), "expecting")
    }

    private fun connectFreshInMemoryDatabase(prefix: String) {
        val dbName = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
        Database.connect(
            url = "jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }
}
