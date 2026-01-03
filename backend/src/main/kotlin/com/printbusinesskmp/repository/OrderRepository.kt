package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class OrderRepository {

    suspend fun allOrders(): List<Order> = dbQuery {
        OrdersTable.selectAll().map { orderRow ->
            val orderId = orderRow[OrdersTable.id]
            val items = getOrderItems(orderId)
            toOrder(orderRow, items)
        }
    }

    suspend fun orderById(id: String): Order? = dbQuery {
        OrdersTable.selectAll().where { OrdersTable.id eq id }
            .map { orderRow ->
                val items = getOrderItems(id)
                toOrder(orderRow, items)
            }
            .singleOrNull()
    }

    suspend fun addOrder(order: Order): Order = dbQuery {
        val insertId = order.id.ifEmpty { UUID.randomUUID().toString() }

        // Insert order
        OrdersTable.insert {
            it[id] = insertId
            it[clientId] = order.clientId
            it[status] = order.status.name
            it[createdAt] = java.time.Instant.now()
            it[updatedAt] = java.time.Instant.now()
            it[completedAt] = null
            it[totalCost] = order.totalCost
            it[totalPrice] = order.totalPrice
            it[totalProfit] = order.totalProfit
            it[notes] = order.notes
            it[invoiceGenerated] = order.invoiceGenerated
        }

        // Insert order items
        order.items.forEach { item ->
            val itemId = item.id.ifEmpty { UUID.randomUUID().toString() }
            OrderItemsTable.insert {
                it[id] = itemId
                it[orderId] = insertId
                it[productType] = item.productType.name
                it[quantity] = item.quantity
                it[size] = item.size
                it[color] = item.color
                it[printArea] = item.printArea.name
                it[designUrl] = item.designUrl
                it[blankItemCost] = item.blankItemCost
                it[thermalPaperCost] = item.thermalPaperCost
                it[laborCost] = item.laborCost
                it[totalCost] = item.totalCost
                it[sellingPrice] = item.sellingPrice
                it[profit] = item.profit
                it[notes] = item.notes
            }
        }

        orderById(insertId)!!
    }

    suspend fun updateOrder(id: String, order: Order): Order? = dbQuery {
        OrdersTable.update({ OrdersTable.id eq id }) {
            it[clientId] = order.clientId
            it[status] = order.status.name
            it[updatedAt] = java.time.Instant.now()
            it[completedAt] = order.completedAt?.let { instant ->
                java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
            }
            it[totalCost] = order.totalCost
            it[totalPrice] = order.totalPrice
            it[totalProfit] = order.totalProfit
            it[notes] = order.notes
            it[invoiceGenerated] = order.invoiceGenerated
        }

        // Delete existing items and insert new ones
        OrderItemsTable.deleteWhere { OrderItemsTable.orderId eq id }
        order.items.forEach { item ->
            val itemId = item.id.ifEmpty { UUID.randomUUID().toString() }
            OrderItemsTable.insert {
                it[OrderItemsTable.id] = itemId
                it[orderId] = id
                it[productType] = item.productType.name
                it[quantity] = item.quantity
                it[size] = item.size
                it[color] = item.color
                it[printArea] = item.printArea.name
                it[designUrl] = item.designUrl
                it[blankItemCost] = item.blankItemCost
                it[thermalPaperCost] = item.thermalPaperCost
                it[laborCost] = item.laborCost
                it[totalCost] = item.totalCost
                it[sellingPrice] = item.sellingPrice
                it[profit] = item.profit
                it[notes] = item.notes
            }
        }

        orderById(id)
    }

    suspend fun updateOrderStatus(id: String, status: OrderStatus): Order? = dbQuery {
        val completedAt = if (status == OrderStatus.COMPLETED) {
            java.time.Instant.now()
        } else {
            null
        }

        OrdersTable.update({ OrdersTable.id eq id }) {
            it[OrdersTable.status] = status.name
            it[updatedAt] = java.time.Instant.now()
            it[OrdersTable.completedAt] = completedAt
        }

        orderById(id)
    }

    suspend fun deleteOrder(id: String): Boolean = dbQuery {
        OrderItemsTable.deleteWhere { orderId eq id }
        OrdersTable.deleteWhere { OrdersTable.id eq id } > 0
    }

    private fun getOrderItems(orderId: String): List<OrderItem> {
        return OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }
            .map { toOrderItem(it) }
    }

    private fun toOrder(row: ResultRow, items: List<OrderItem>): Order =
        Order(
            id = row[OrdersTable.id],
            clientId = row[OrdersTable.clientId],
            items = items,
            status = OrderStatus.valueOf(row[OrdersTable.status]),
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(
                row[OrdersTable.createdAt].toEpochMilli()
            ),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(
                row[OrdersTable.updatedAt].toEpochMilli()
            ),
            completedAt = row[OrdersTable.completedAt]?.let {
                kotlin.time.Instant.fromEpochMilliseconds(it.toEpochMilli())
            },
            totalCost = row[OrdersTable.totalCost],
            totalPrice = row[OrdersTable.totalPrice],
            totalProfit = row[OrdersTable.totalProfit],
            notes = row[OrdersTable.notes],
            invoiceGenerated = row[OrdersTable.invoiceGenerated]
        )

    private fun toOrderItem(row: ResultRow): OrderItem =
        OrderItem(
            id = row[OrderItemsTable.id],
            productType = ProductType.valueOf(row[OrderItemsTable.productType]),
            quantity = row[OrderItemsTable.quantity],
            size = row[OrderItemsTable.size],
            color = row[OrderItemsTable.color],
            printArea = PrintArea.valueOf(row[OrderItemsTable.printArea]),
            designUrl = row[OrderItemsTable.designUrl],
            blankItemCost = row[OrderItemsTable.blankItemCost],
            thermalPaperCost = row[OrderItemsTable.thermalPaperCost],
            laborCost = row[OrderItemsTable.laborCost],
            totalCost = row[OrderItemsTable.totalCost],
            sellingPrice = row[OrderItemsTable.sellingPrice],
            profit = row[OrderItemsTable.profit],
            notes = row[OrderItemsTable.notes]
        )
}