package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.LayoutsTable
import com.printbusinesskmp.models.Layout
import com.printbusinesskmp.models.LayoutCreateRequest
import com.printbusinesskmp.models.LayoutStatus
import com.printbusinesskmp.models.LayoutUpdateRequest
import com.printbusinesskmp.models.ServiceType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class LayoutRepository {

    suspend fun allLayouts(
        search: String? = null,
        clientId: String? = null,
        status: LayoutStatus? = null
    ): List<Layout> = dbQuery {
        val query = LayoutsTable
            .join(
                otherTable = ClientsTable,
                joinType = JoinType.LEFT,
                onColumn = LayoutsTable.clientId,
                otherColumn = ClientsTable.id
            )
            .selectAll()

        clientId?.trim()?.takeIf { it.isNotEmpty() }?.let { filterClientId ->
            query.andWhere { LayoutsTable.clientId eq filterClientId }
        }

        status?.let { filterStatus ->
            query.andWhere { LayoutsTable.status eq filterStatus.name }
        }

        search?.trim()?.takeIf { it.isNotEmpty() }?.let { filterText ->
            val pattern = "%$filterText%"
            query.andWhere {
                (LayoutsTable.name like pattern) or
                    (LayoutsTable.notes like pattern) or
                    (ClientsTable.displayName like pattern)
            }
        }

        query
            .orderBy(LayoutsTable.updatedAt to SortOrder.DESC)
            .map(::toLayout)
    }

    suspend fun layoutById(id: String): Layout? = dbQuery {
        LayoutsTable.selectAll()
            .where { LayoutsTable.id eq id }
            .map(::toLayout)
            .singleOrNull()
    }

    suspend fun addLayout(request: LayoutCreateRequest): Layout = dbQuery {
        validateRequest(
            clientId = request.clientId,
            name = request.name,
            widthCm = request.widthCm,
            heightCm = request.heightCm,
            dpi = request.dpi
        )

        val id = UUID.randomUUID().toString()
        val now = Instant.now()

        LayoutsTable.insert {
            it[LayoutsTable.id] = id
            it[LayoutsTable.clientId] = request.clientId?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[name] = request.name.trim()
            it[serviceType] = request.serviceType.name
            it[status] = request.status.name
            it[widthCm] = request.widthCm
            it[heightCm] = request.heightCm
            it[dpi] = request.dpi
            it[previewUrl] = request.previewUrl?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[createdAt] = now
            it[updatedAt] = now
        }

        LayoutsTable.selectAll()
            .where { LayoutsTable.id eq id }
            .map(::toLayout)
            .single()
    }

    suspend fun updateLayout(id: String, request: LayoutUpdateRequest): Layout? = dbQuery {
        validateRequest(
            clientId = request.clientId,
            name = request.name,
            widthCm = request.widthCm,
            heightCm = request.heightCm,
            dpi = request.dpi
        )

        val changed = LayoutsTable.update({ LayoutsTable.id eq id }) {
            it[LayoutsTable.clientId] = request.clientId?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[name] = request.name.trim()
            it[serviceType] = request.serviceType.name
            it[status] = request.status.name
            it[widthCm] = request.widthCm
            it[heightCm] = request.heightCm
            it[dpi] = request.dpi
            it[previewUrl] = request.previewUrl?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[updatedAt] = Instant.now()
        }

        if (changed == 0) {
            null
        } else {
            LayoutsTable.selectAll()
                .where { LayoutsTable.id eq id }
                .map(::toLayout)
                .singleOrNull()
        }
    }

    suspend fun deleteLayout(id: String): Boolean = dbQuery {
        LayoutsTable.deleteWhere { LayoutsTable.id eq id } > 0
    }

    private fun validateRequest(
        clientId: String?,
        name: String,
        widthCm: Double,
        heightCm: Double,
        dpi: Int
    ) {
        if (name.isBlank()) {
            throw IllegalArgumentException("Назва макета є обов'язковою")
        }
        if (widthCm <= 0.0) {
            throw IllegalArgumentException("Ширина макета повинна бути більше нуля")
        }
        if (heightCm <= 0.0) {
            throw IllegalArgumentException("Висота макета повинна бути більше нуля")
        }
        if (dpi <= 0) {
            throw IllegalArgumentException("DPI повинен бути більше нуля")
        }

        val targetClientId = clientId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val clientExists = ClientsTable.selectAll().where { ClientsTable.id eq targetClientId }.count() > 0
        if (!clientExists) {
            throw IllegalArgumentException("Клієнта не знайдено")
        }
    }

    private fun toLayout(row: ResultRow): Layout {
        return Layout(
            id = row[LayoutsTable.id],
            clientId = row[LayoutsTable.clientId],
            name = row[LayoutsTable.name],
            serviceType = ServiceType.valueOf(row[LayoutsTable.serviceType]),
            status = LayoutStatus.valueOf(row[LayoutsTable.status]),
            widthCm = row[LayoutsTable.widthCm],
            heightCm = row[LayoutsTable.heightCm],
            dpi = row[LayoutsTable.dpi],
            previewUrl = row[LayoutsTable.previewUrl],
            notes = row[LayoutsTable.notes],
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[LayoutsTable.createdAt].toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[LayoutsTable.updatedAt].toEpochMilli())
        )
    }
}
