package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Delivery(
    val id: String = "",
    val orderId: String = "",
    val method: DeliveryMethod = DeliveryMethod.PICKUP,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val trackingNumber: String? = null,
    val cost: Double = 0.0,
    val paidByClient: Boolean = true,
    val address: String? = null,
    val city: String? = null,
    val warehouseNumber: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val shippedAt: Instant? = null,
    val deliveredAt: Instant? = null,
    val notes: String? = null,
    val createdAt: Instant? = null,
)

@Serializable
data class DeliveryCreateRequest(
    val orderId: String,
    val method: DeliveryMethod = DeliveryMethod.PICKUP,
    val trackingNumber: String? = null,
    val cost: Double = 0.0,
    val paidByClient: Boolean = true,
    val address: String? = null,
    val city: String? = null,
    val warehouseNumber: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val notes: String? = null,
)
