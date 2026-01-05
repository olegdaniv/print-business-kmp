package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.*

class ClientRepository {

    suspend fun allClients(): List<Client> = dbQuery {
        ClientsTable.selectAll().map { toClient(it) }
    }

    suspend fun clientById(id: String): Client? = dbQuery {
        ClientsTable.selectAll().where { ClientsTable.id eq id }
            .map { toClient(it) }
            .singleOrNull()
    }

    suspend fun addClient(request: ClientCreateRequest): Client = dbQuery {
        val insertId = UUID.randomUUID().toString()

        ClientsTable.insert {
            it[id] = insertId
            it[name] = request.name
            it[phone] = request.phone
            it[email] = request.email
            it[totalOrders] = 0
            it[createdAt] = Instant.now()
        }

        // Query directly within same transaction to avoid nested transaction issue
        ClientsTable.selectAll().where { ClientsTable.id eq insertId }
            .map { toClient(it) }
            .single()
    }

    suspend fun updateClient(id: String, request: ClientUpdateRequest): Client? = dbQuery {
        ClientsTable.update({ ClientsTable.id eq id }) {
            it[name] = request.name
            it[phone] = request.phone
            it[email] = request.email
        }
        clientById(id)
    }

    suspend fun deleteClient(id: String): Boolean = dbQuery {
        ClientsTable.deleteWhere { ClientsTable.id eq id } > 0
    }

    private fun toClient(row: ResultRow): Client =
        Client(
            id = row[ClientsTable.id],
            name = row[ClientsTable.name],
            phone = row[ClientsTable.phone],
            email = row[ClientsTable.email],
            totalOrders = row[ClientsTable.totalOrders],
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(
                row[ClientsTable.createdAt].toEpochMilli()
            )
        )
}