package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderItem
import com.printbusinesskmp.models.OrderItemDraft
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PricingConfig
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.utils.PricingCalculator
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class OrderRepository {

    suspend fun allOrders(): List<Order> = dbQuery {
        OrdersTable.selectAll().map { row ->
            val orderId = row[OrdersTable.id]
            toOrder(row, getOrderItems(orderId))
        }
    }

    suspend fun orderById(id: String): Order? = dbQuery {
        OrdersTable.selectAll()
            .where { OrdersTable.id eq id }
            .map { row -> toOrder(row, getOrderItems(id)) }
            .singleOrNull()
    }

    suspend fun addOrder(request: OrderCreateRequest): Order = dbQuery {
        validateOrderRequest(request.clientId, request.items)

        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        val items = request.items.map { draft -> toCalculatedItem(draft) }

        OrdersTable.insert {
            it[OrdersTable.id] = id
            it[clientId] = request.clientId
            it[status] = request.status.name
            it[paymentStatus] = request.paymentStatus.name
            it[totalCost] = items.sumOf { item -> item.cost }
            it[totalPrice] = items.sumOf { item -> item.price }
            it[profit] = items.sumOf { item -> item.profit }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[createdAt] = now
            it[updatedAt] = now
        }

        insertOrderItems(id, items)

        val orderRow = OrdersTable.selectAll().where { OrdersTable.id eq id }.single()
        toOrder(orderRow, getOrderItems(id))
    }

    suspend fun updateOrder(id: String, request: OrderUpdateRequest): Order? = dbQuery {
        val existingOrder = OrdersTable.selectAll()
            .where { OrdersTable.id eq id }
            .singleOrNull() ?: return@dbQuery null

        validateOrderRequest(request.clientId, request.items)
        val items = request.items.map { draft -> toCalculatedItem(draft) }

        OrdersTable.update({ OrdersTable.id eq id }) {
            it[clientId] = request.clientId
            it[status] = request.status.name
            it[paymentStatus] = request.paymentStatus.name
            it[totalCost] = items.sumOf { item -> item.cost }
            it[totalPrice] = items.sumOf { item -> item.price }
            it[profit] = items.sumOf { item -> item.profit }
            it[notes] = request.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
            it[updatedAt] = Instant.now()
            it[createdAt] = existingOrder[OrdersTable.createdAt]
        }

        OrderItemsTable.deleteWhere { OrderItemsTable.orderId eq id }
        insertOrderItems(id, items)

        val orderRow = OrdersTable.selectAll().where { OrdersTable.id eq id }.single()
        toOrder(orderRow, getOrderItems(id))
    }

    suspend fun updateStatus(
        id: String,
        status: OrderStatus,
        paymentStatus: PaymentStatus?
    ): Order? = dbQuery {
        val changed = OrdersTable.update({ OrdersTable.id eq id }) {
            it[OrdersTable.status] = status.name
            paymentStatus?.let { value ->
                it[OrdersTable.paymentStatus] = value.name
            }
            it[updatedAt] = Instant.now()
        }

        if (changed == 0) {
            null
        } else {
            val row = OrdersTable.selectAll().where { OrdersTable.id eq id }.single()
            toOrder(row, getOrderItems(id))
        }
    }

    suspend fun deleteOrder(id: String): Boolean = dbQuery {
        OrderItemsTable.deleteWhere { orderId eq id }
        OrdersTable.deleteWhere { OrdersTable.id eq id } > 0
    }

    private fun validateOrderRequest(clientId: String, items: List<OrderItemDraft>) {
        if (clientId.isBlank()) {
            throw IllegalArgumentException("Клієнт є обов'язковим")
        }

        if (items.isEmpty()) {
            throw IllegalArgumentException("Замовлення повинно містити хоча б одну позицію")
        }

        val clientExists = ClientsTable.selectAll().where { ClientsTable.id eq clientId }.count() > 0
        if (!clientExists) {
            throw IllegalArgumentException("Клієнта не знайдено")
        }

        items.forEachIndexed { index, item ->
            if (item.quantity <= 0) {
                throw IllegalArgumentException("Позиція #${index + 1}: кількість повинна бути більше нуля")
            }
            if (item.usedMeters <= 0.0) {
                throw IllegalArgumentException("Позиція #${index + 1}: метраж повинен бути більше нуля")
            }
            val manualPrice = item.manualPrice
            if (manualPrice != null && manualPrice <= 0.0) {
                throw IllegalArgumentException("Позиція #${index + 1}: ручна ціна повинна бути більше нуля")
            }
        }
    }

    private fun toCalculatedItem(draft: OrderItemDraft): OrderItem {
        val pricing = PricingCalculator.calculate(draft)
        if (pricing.finalPrice <= 0.0) {
            throw IllegalArgumentException("Ціна позиції не може бути нульовою")
        }

        return OrderItem(
            id = UUID.randomUUID().toString(),
            serviceType = draft.serviceType,
            productType = draft.productType,
            quantity = draft.quantity,
            usedMeters = draft.usedMeters,
            garmentCost = draft.garmentCost,
            pricing = draft.pricing,
            manualPrice = draft.manualPrice,
            cost = pricing.totalCost,
            price = pricing.finalPrice,
            taxAmount = pricing.taxAmount,
            profit = pricing.profit,
            notes = draft.notes?.trim()?.takeIf { value -> value.isNotEmpty() }
        )
    }

    private fun insertOrderItems(orderId: String, items: List<OrderItem>) {
        items.forEach { item ->
            OrderItemsTable.insert {
                it[id] = item.id
                it[OrderItemsTable.orderId] = orderId
                it[serviceType] = item.serviceType.name
                it[productType] = item.productType.name
                it[quantity] = item.quantity
                it[usedMeters] = item.usedMeters
                it[garmentCost] = item.garmentCost
                it[costPerMeter] = item.pricing.costPerMeter
                it[overheadPerOrder] = item.pricing.overheadPerOrder
                it[wastePercent] = item.pricing.wastePercent
                it[setupFee] = item.pricing.setupFee
                it[minOrderPrice] = item.pricing.minOrderPrice
                it[marginPercent] = item.pricing.marginPercent
                it[taxPercent] = item.pricing.taxPercent
                it[manualPrice] = item.manualPrice
                it[cost] = item.cost
                it[price] = item.price
                it[taxAmount] = item.taxAmount
                it[profit] = item.profit
                it[notes] = item.notes
            }
        }
    }

    private fun getOrderItems(orderId: String): List<OrderItem> {
        return OrderItemsTable.selectAll()
            .where { OrderItemsTable.orderId eq orderId }
            .map(::toOrderItem)
    }

    private fun toOrder(row: ResultRow, items: List<OrderItem>): Order {
        return Order(
            id = row[OrdersTable.id],
            clientId = row[OrdersTable.clientId],
            status = OrderStatus.valueOf(row[OrdersTable.status]),
            paymentStatus = PaymentStatus.valueOf(row[OrdersTable.paymentStatus]),
            items = items,
            totalCost = row[OrdersTable.totalCost],
            totalPrice = row[OrdersTable.totalPrice],
            profit = row[OrdersTable.profit],
            notes = row[OrdersTable.notes],
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[OrdersTable.createdAt].toEpochMilli()),
            updatedAt = kotlin.time.Instant.fromEpochMilliseconds(row[OrdersTable.updatedAt].toEpochMilli())
        )
    }

    private fun toOrderItem(row: ResultRow): OrderItem {
        return OrderItem(
            id = row[OrderItemsTable.id],
            serviceType = ServiceType.valueOf(row[OrderItemsTable.serviceType]),
            productType = ProductType.valueOf(row[OrderItemsTable.productType]),
            quantity = row[OrderItemsTable.quantity],
            usedMeters = row[OrderItemsTable.usedMeters],
            garmentCost = row[OrderItemsTable.garmentCost],
            pricing = PricingConfig(
                costPerMeter = row[OrderItemsTable.costPerMeter],
                overheadPerOrder = row[OrderItemsTable.overheadPerOrder],
                wastePercent = row[OrderItemsTable.wastePercent],
                setupFee = row[OrderItemsTable.setupFee],
                minOrderPrice = row[OrderItemsTable.minOrderPrice],
                marginPercent = row[OrderItemsTable.marginPercent],
                taxPercent = row[OrderItemsTable.taxPercent]
            ),
            manualPrice = row[OrderItemsTable.manualPrice],
            cost = row[OrderItemsTable.cost],
            price = row[OrderItemsTable.price],
            taxAmount = row[OrderItemsTable.taxAmount],
            profit = row[OrderItemsTable.profit],
            notes = row[OrderItemsTable.notes]
        )
    }
}
