package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.SavedItemsTable
import com.printbusinesskmp.models.SavedItem
import com.printbusinesskmp.models.SavedItemCreateRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class SavedItemRepository {

    suspend fun all(): List<SavedItem> = dbQuery {
        SavedItemsTable.selectAll()
            .orderBy(SavedItemsTable.name)
            .map(::toSavedItem)
    }

    suspend fun add(request: SavedItemCreateRequest): SavedItem = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        SavedItemsTable.insert {
            it[SavedItemsTable.id] = id
            it[name] = request.name.trim()
            it[unit] = request.unit.trim().ifBlank { "шт." }
            it[defaultPrice] = request.defaultPrice.coerceAtLeast(0.0)
            it[createdAt] = now
        }
        SavedItemsTable.selectAll()
            .where { SavedItemsTable.id eq id }
            .single()
            .let(::toSavedItem)
    }

    suspend fun upsertByName(request: SavedItemCreateRequest): SavedItem = dbQuery {
        val existing = SavedItemsTable.selectAll()
            .where { SavedItemsTable.name eq request.name.trim() }
            .singleOrNull()

        if (existing != null) {
            toSavedItem(existing)
        } else {
            val id = UUID.randomUUID().toString()
            SavedItemsTable.insert {
                it[SavedItemsTable.id] = id
                it[name] = request.name.trim()
                it[unit] = request.unit.trim().ifBlank { "шт." }
                it[defaultPrice] = request.defaultPrice.coerceAtLeast(0.0)
                it[createdAt] = Instant.now()
            }
            SavedItemsTable.selectAll()
                .where { SavedItemsTable.id eq id }
                .single()
                .let(::toSavedItem)
        }
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        SavedItemsTable.deleteWhere { SavedItemsTable.id eq id } > 0
    }

    private fun toSavedItem(row: ResultRow): SavedItem = SavedItem(
        id = row[SavedItemsTable.id],
        name = row[SavedItemsTable.name],
        unit = row[SavedItemsTable.unit],
        defaultPrice = row[SavedItemsTable.defaultPrice],
        createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[SavedItemsTable.createdAt].toEpochMilli())
    )
}
