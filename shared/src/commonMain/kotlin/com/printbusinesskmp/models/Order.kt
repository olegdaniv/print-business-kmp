package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val clientId: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    @Contextual val completedAt: Instant? = null,
    val totalCost: Double,
    val totalPrice: Double,
    val totalProfit: Double,
    val notes: String? = null,
    val invoiceGenerated: Boolean = false
) {
    companion object {
        fun create(
            id: String,
            clientId: String,
            items: List<OrderItem>,
            status: OrderStatus,
            createdAt: Instant,
            updatedAt: Instant,
            completedAt: Instant? = null,
            notes: String? = null,
            invoiceGenerated: Boolean = false
        ): Order {
            val totalCost = items.sumOf { it.totalCost }
            val totalPrice = items.sumOf { it.sellingPrice }
            val totalProfit = totalPrice - totalCost

            return Order(
                id = id,
                clientId = clientId,
                items = items,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                completedAt = completedAt,
                totalCost = totalCost,
                totalPrice = totalPrice,
                totalProfit = totalProfit,
                notes = notes,
                invoiceGenerated = invoiceGenerated
            )
        }
    }
}