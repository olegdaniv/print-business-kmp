package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.PartnersTable
import com.printbusinesskmp.models.Partner
import com.printbusinesskmp.models.PartnerCreateRequest
import com.printbusinesskmp.models.PartnerType
import com.printbusinesskmp.models.PartnerUpdateRequest
import com.printbusinesskmp.models.modelsJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class PartnerRepository {

    suspend fun allPartners(): List<Partner> = dbQuery {
        PartnersTable.selectAll().map(::toPartner)
    }

    suspend fun partnerById(id: String): Partner? = dbQuery {
        PartnersTable.selectAll()
            .where { PartnersTable.id eq id }
            .map(::toPartner)
            .singleOrNull()
    }

    suspend fun addPartner(request: PartnerCreateRequest): Partner = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()

        PartnersTable.insert {
            it[PartnersTable.id] = id
            it[type] = request.type.name
            it[name] = request.name.trim()
            it[phone] = request.phone?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[email] = request.email?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[address] = request.address?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[services] = modelsJson.encodeToString(request.services)
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[createdAt] = now
            it[updatedAt] = now
        }

        PartnersTable.selectAll().where { PartnersTable.id eq id }
            .map(::toPartner)
            .single()
    }

    suspend fun updatePartner(id: String, request: PartnerUpdateRequest): Partner? = dbQuery {
        val changed = PartnersTable.update({ PartnersTable.id eq id }) {
            it[type] = request.type.name
            it[name] = request.name.trim()
            it[phone] = request.phone?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[email] = request.email?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[address] = request.address?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[services] = modelsJson.encodeToString(request.services)
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[updatedAt] = Instant.now()
        }

        if (changed == 0) {
            null
        } else {
            PartnersTable.selectAll().where { PartnersTable.id eq id }
                .map(::toPartner)
                .singleOrNull()
        }
    }

    suspend fun deletePartner(id: String): Boolean = dbQuery {
        PartnersTable.deleteWhere { PartnersTable.id eq id } > 0
    }

    private fun toPartner(row: ResultRow): Partner {
        return Partner(
            id = row[PartnersTable.id],
            type = PartnerType.valueOf(row[PartnersTable.type]),
            name = row[PartnersTable.name],
            phone = row[PartnersTable.phone],
            email = row[PartnersTable.email],
            address = row[PartnersTable.address],
            services = runCatching { modelsJson.decodeFromString<List<String>>(row[PartnersTable.services]) }
                .getOrDefault(emptyList()),
            notes = row[PartnersTable.notes],
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[PartnersTable.createdAt].toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[PartnersTable.updatedAt].toEpochMilli())
        )
    }
}
