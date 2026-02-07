package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val clientId: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val items: List<OrderItem>,
    val totalCost: Double,
    val totalPrice: Double,
    val profit: Double,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class OrderCreateRequest(
    val clientId: String,
    val status: OrderStatus = OrderStatus.NEW,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val items: List<OrderItemDraft>,
    val notes: String? = null
)

@Serializable
data class OrderUpdateRequest(
    val clientId: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val items: List<OrderItemDraft>,
    val notes: String? = null
)
