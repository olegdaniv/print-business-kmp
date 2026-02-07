package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.BusinessProfilesTable
import com.printbusinesskmp.models.BusinessProfile
import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class BusinessProfileRepository {

    suspend fun getProfile(): BusinessProfile? = dbQuery {
        BusinessProfilesTable.selectAll()
            .limit(1)
            .map(::toProfile)
            .singleOrNull()
    }

    suspend fun upsert(request: BusinessProfileUpsertRequest): BusinessProfile = dbQuery {
        val existing = BusinessProfilesTable.selectAll().limit(1).singleOrNull()
        val now = Instant.now()

        if (existing == null) {
            val id = UUID.randomUUID().toString()
            BusinessProfilesTable.insert {
                it[BusinessProfilesTable.id] = id
                it[ownerName] = request.ownerName.trim()
                it[taxId] = request.taxId.trim()
                it[address] = request.address.trim()
                it[iban] = request.iban.trim()
                it[bankName] = request.bankName.trim()
                it[taxPercent] = request.taxPercent
                it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
                it[updatedAt] = now
            }

            BusinessProfilesTable.selectAll()
                .where { BusinessProfilesTable.id eq id }
                .map(::toProfile)
                .single()
        } else {
            val id = existing[BusinessProfilesTable.id]
            BusinessProfilesTable.update({ BusinessProfilesTable.id eq id }) {
                it[ownerName] = request.ownerName.trim()
                it[taxId] = request.taxId.trim()
                it[address] = request.address.trim()
                it[iban] = request.iban.trim()
                it[bankName] = request.bankName.trim()
                it[taxPercent] = request.taxPercent
                it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
                it[updatedAt] = now
            }

            BusinessProfilesTable.selectAll()
                .where { BusinessProfilesTable.id eq id }
                .map(::toProfile)
                .single()
        }
    }

    private fun toProfile(row: ResultRow): BusinessProfile {
        return BusinessProfile(
            id = row[BusinessProfilesTable.id],
            ownerName = row[BusinessProfilesTable.ownerName],
            taxId = row[BusinessProfilesTable.taxId],
            address = row[BusinessProfilesTable.address],
            iban = row[BusinessProfilesTable.iban],
            bankName = row[BusinessProfilesTable.bankName],
            taxPercent = row[BusinessProfilesTable.taxPercent],
            notes = row[BusinessProfilesTable.notes],
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[BusinessProfilesTable.updatedAt].toEpochMilli())
        )
    }
}
