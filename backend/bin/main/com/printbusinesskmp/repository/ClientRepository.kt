package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientDelivery
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.models.DeliveryType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ClientRepository {

    suspend fun allClients(): List<Client> = dbQuery {
        val orderCountExpr = OrdersTable.id.count()
        val orderCounts = OrdersTable
            .select(OrdersTable.clientId, orderCountExpr)
            .groupBy(OrdersTable.clientId)
            .associate { row ->
                row[OrdersTable.clientId] to row[orderCountExpr].toInt()
            }

        ClientsTable.selectAll().map { row ->
            val clientId = row[ClientsTable.id]
            toClient(row, orderCounts[clientId] ?: 0)
        }
    }

    suspend fun clientById(id: String): Client? = dbQuery {
        val orderCount = OrdersTable.selectAll().where { OrdersTable.clientId eq id }.count().toInt()

        ClientsTable.selectAll()
            .where { ClientsTable.id eq id }
            .map { toClient(it, orderCount) }
            .singleOrNull()
    }

    suspend fun addClient(request: ClientCreateRequest): Client = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Instant.now()

        ClientsTable.insert {
            it[ClientsTable.id] = id
            it[type] = request.type.name
            it[displayName] = request.displayName.trim()
            it[contactName] = request.contactName?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[phone] = request.phone.trim()
            it[email] = request.email?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[taxId] = request.taxId?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[address] = request.address.trim()
            it[iban] = request.iban?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[bankName] = request.bankName?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryType] = request.delivery?.type?.name
            it[deliveryCity] = request.delivery?.city?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryBranch] = request.delivery?.branch?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryStreet] = request.delivery?.street?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryBuilding] = request.delivery?.building?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryFreeAddress] = request.delivery?.freeAddress?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[createdAt] = now
            it[updatedAt] = now
        }

        ClientsTable.selectAll()
            .where { ClientsTable.id eq id }
            .map { toClient(it, 0) }
            .single()
    }

    suspend fun updateClient(id: String, request: ClientUpdateRequest): Client? = dbQuery {
        val updatedRows = ClientsTable.update({ ClientsTable.id eq id }) {
            it[type] = request.type.name
            it[displayName] = request.displayName.trim()
            it[contactName] = request.contactName?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[phone] = request.phone.trim()
            it[email] = request.email?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[taxId] = request.taxId?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[address] = request.address.trim()
            it[iban] = request.iban?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[bankName] = request.bankName?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryType] = request.delivery?.type?.name
            it[deliveryCity] = request.delivery?.city?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryBranch] = request.delivery?.branch?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryStreet] = request.delivery?.street?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryBuilding] = request.delivery?.building?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[deliveryFreeAddress] = request.delivery?.freeAddress?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[updatedAt] = Instant.now()
        }

        if (updatedRows == 0) {
            null
        } else {
            val orderCount = OrdersTable.selectAll().where { OrdersTable.clientId eq id }.count().toInt()
            ClientsTable.selectAll()
                .where { ClientsTable.id eq id }
                .map { toClient(it, orderCount) }
                .singleOrNull()
        }
    }

    suspend fun deleteClient(id: String): Boolean = dbQuery {
        val hasOrders = OrdersTable.selectAll().where { OrdersTable.clientId eq id }.count() > 0
        if (hasOrders) {
            throw IllegalStateException("Неможливо видалити клієнта, у якого є замовлення")
        }

        ClientsTable.deleteWhere { ClientsTable.id eq id } > 0
    }

    private fun toClient(row: ResultRow, orderCount: Int): Client {
        val deliveryTypeName = row[ClientsTable.deliveryType]
        val delivery = if (deliveryTypeName != null) {
            ClientDelivery(
                type = DeliveryType.valueOf(deliveryTypeName),
                city = row[ClientsTable.deliveryCity],
                branch = row[ClientsTable.deliveryBranch],
                street = row[ClientsTable.deliveryStreet],
                building = row[ClientsTable.deliveryBuilding],
                freeAddress = row[ClientsTable.deliveryFreeAddress]
            )
        } else null

        return Client(
            id = row[ClientsTable.id],
            type = ClientType.valueOf(row[ClientsTable.type]),
            displayName = row[ClientsTable.displayName],
            contactName = row[ClientsTable.contactName],
            phone = row[ClientsTable.phone],
            email = row[ClientsTable.email],
            taxId = row[ClientsTable.taxId],
            address = row[ClientsTable.address],
            iban = row[ClientsTable.iban],
            bankName = row[ClientsTable.bankName],
            notes = row[ClientsTable.notes],
            orderCount = orderCount,
            delivery = delivery,
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[ClientsTable.createdAt].toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[ClientsTable.updatedAt].toEpochMilli())
        )
    }
}
