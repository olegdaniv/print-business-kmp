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
    val discountAmount: Double = 0.0,
    val outsourceOrderIds: List<String> = emptyList(),
    val deliveryMethod: DeliveryMethod? = null,
    val deliveryId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class OrderCreateRequest(
    val clientId: String,
    val status: OrderStatus = OrderStatus.DRAFT,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val items: List<OrderItemDraft>,
    val notes: String? = null,
    val discountAmount: Double = 0.0,
    val deliveryMethod: DeliveryMethod? = null
)

@Serializable
data class OrderUpdateRequest(
    val clientId: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val items: List<OrderItemDraft>,
    val notes: String? = null,
    val discountAmount: Double = 0.0,
    val outsourceOrderIds: List<String> = emptyList(),
    val deliveryMethod: DeliveryMethod? = null,
    val deliveryId: String? = null
)
