package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.models.Client
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

    suspend fun addClient(client: Client): Client = dbQuery {
        val insertId = client.id.ifEmpty { UUID.randomUUID().toString() }

        ClientsTable.insert {
            it[id] = insertId
            it[name] = client.name
            it[phone] = client.phone
            it[email] = client.email
            it[totalOrders] = client.totalOrders
            it[createdAt] = java.time.Instant.now()
        }

        clientById(insertId)!!
    }

    suspend fun updateClient(id: String, client: Client): Client? = dbQuery {
        ClientsTable.update({ ClientsTable.id eq id }) {
            it[name] = client.name
            it[phone] = client.phone
            it[email] = client.email
            it[totalOrders] = client.totalOrders
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