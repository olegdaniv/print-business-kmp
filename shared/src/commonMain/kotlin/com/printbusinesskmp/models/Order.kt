package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val clientId: String,
    val status: OrderStatus,
    /** Derived from received payments, see [PaymentStatus.from]. */
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
    /** Sum of all payment allocations to this order. */
    val paidAmount: Double = 0.0,
    val lastPaidAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Amount the client still owes (negative when overpaid). */
    val balanceDue: Double get() = totalPrice - paidAmount
}

@Serializable
data class OrderCreateRequest(
    val clientId: String,
    val status: OrderStatus = OrderStatus.DRAFT,
    val items: List<OrderItemDraft>,
    val notes: String? = null,
    val discountAmount: Double = 0.0,
    val deliveryMethod: DeliveryMethod? = null
)

@Serializable
data class OrderUpdateRequest(
    val clientId: String,
    val status: OrderStatus,
    val items: List<OrderItemDraft>,
    val notes: String? = null,
    val discountAmount: Double = 0.0,
    val outsourceOrderIds: List<String> = emptyList(),
    val deliveryMethod: DeliveryMethod? = null,
    val deliveryId: String? = null
)
