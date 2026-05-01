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
                it[email] = request.email?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[phone] = request.phone?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[edrpou] = request.edrpou.filter { c -> c.isDigit() }
                it[ipn] = request.ipn?.filter { c -> c.isDigit() }?.takeIf { v -> v.isNotEmpty() }
                it[address] = request.address.trim()
                it[iban] = request.iban.replace(" ", "").uppercase()
                it[bankName] = request.bankName?.trim().orEmpty()
                it[mfo] = request.mfo?.filter { c -> c.isDigit() }?.takeIf { v -> v.isNotEmpty() }
                it[taxNote] = request.taxNote?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[certificateNumber] = request.certificateNumber?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[taxPercent] = request.taxPercent
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
                it[email] = request.email?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[phone] = request.phone?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[edrpou] = request.edrpou.filter { c -> c.isDigit() }
                it[ipn] = request.ipn?.filter { c -> c.isDigit() }?.takeIf { v -> v.isNotEmpty() }
                it[address] = request.address.trim()
                it[iban] = request.iban.replace(" ", "").uppercase()
                it[bankName] = request.bankName?.trim().orEmpty()
                it[mfo] = request.mfo?.filter { c -> c.isDigit() }?.takeIf { v -> v.isNotEmpty() }
                it[taxNote] = request.taxNote?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[certificateNumber] = request.certificateNumber?.trim()?.takeIf { v -> v.isNotEmpty() }
                it[taxPercent] = request.taxPercent
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
            email = row[BusinessProfilesTable.email],
            phone = row[BusinessProfilesTable.phone],
            edrpou = row[BusinessProfilesTable.edrpou],
            ipn = row[BusinessProfilesTable.ipn],
            address = row[BusinessProfilesTable.address],
            iban = row[BusinessProfilesTable.iban],
            bankName = row[BusinessProfilesTable.bankName].takeIf { it.isNotEmpty() },
            mfo = row[BusinessProfilesTable.mfo],
            taxNote = row[BusinessProfilesTable.taxNote],
            certificateNumber = row[BusinessProfilesTable.certificateNumber],
            taxPercent = row[BusinessProfilesTable.taxPercent],
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[BusinessProfilesTable.updatedAt].toEpochMilli())
        )
    }
}
