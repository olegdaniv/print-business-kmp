package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    NEW,
    IN_PRODUCTION,
    READY,
    COMPLETED,
    CANCELLED
}

@Serializable
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID
}
