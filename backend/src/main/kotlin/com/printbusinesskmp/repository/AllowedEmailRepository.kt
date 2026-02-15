package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.AllowedEmailsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.UUID

data class AllowedEmailRecord(
    val id: String,
    val email: String,
    val createdAt: Instant,
    val note: String? = null
)

class AllowedEmailRepository {

    suspend fun isAllowed(normalizedEmail: String): Boolean = dbQuery {
        AllowedEmailsTable.selectAll()
            .where { AllowedEmailsTable.email eq normalizedEmail }
            .limit(1)
            .count() > 0
    }

    suspend fun findByEmail(normalizedEmail: String): AllowedEmailRecord? = dbQuery {
        AllowedEmailsTable.selectAll()
            .where { AllowedEmailsTable.email eq normalizedEmail }
            .map(::toRecord)
            .singleOrNull()
    }

    suspend fun addEmail(normalizedEmail: String, note: String?): AllowedEmailRecord = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()

        AllowedEmailsTable.insert {
            it[AllowedEmailsTable.id] = id
            it[email] = normalizedEmail
            it[createdAt] = now
            it[AllowedEmailsTable.note] = note
        }

        AllowedEmailsTable.selectAll()
            .where { AllowedEmailsTable.id eq id }
            .map(::toRecord)
            .single()
    }

    suspend fun removeEmail(normalizedEmail: String): Boolean = dbQuery {
        AllowedEmailsTable.deleteWhere { AllowedEmailsTable.email eq normalizedEmail } > 0
    }

    suspend fun listEmails(): List<AllowedEmailRecord> = dbQuery {
        AllowedEmailsTable.selectAll()
            .orderBy(AllowedEmailsTable.createdAt to SortOrder.ASC)
            .map(::toRecord)
    }

    private fun toRecord(row: ResultRow): AllowedEmailRecord {
        return AllowedEmailRecord(
            id = row[AllowedEmailsTable.id],
            email = row[AllowedEmailsTable.email],
            createdAt = row[AllowedEmailsTable.createdAt],
            note = row[AllowedEmailsTable.note]
        )
    }
}
