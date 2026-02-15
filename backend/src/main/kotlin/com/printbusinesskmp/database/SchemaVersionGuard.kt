package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.SchemaVersionTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant

internal object SchemaVersionGuard {
    const val EXPECTED_SCHEMA_VERSION: Int = 1
    private const val ROW_ID = "main"

    fun ensureCompatible() {
        SchemaUtils.createMissingTablesAndColumns(SchemaVersionTable)

        val existing = SchemaVersionTable.selectAll().singleOrNull()
        if (existing == null) {
            SchemaVersionTable.insert {
                it[id] = ROW_ID
                it[version] = EXPECTED_SCHEMA_VERSION
                it[updatedAt] = Instant.now()
            }
            return
        }

        val currentVersion = existing[SchemaVersionTable.version]
        if (currentVersion != EXPECTED_SCHEMA_VERSION) {
            throw IllegalStateException(
                "Database schema version $currentVersion is incompatible with application version expecting " +
                    "$EXPECTED_SCHEMA_VERSION. Backup DB and run migration/reset before startup."
            )
        }
    }
}
