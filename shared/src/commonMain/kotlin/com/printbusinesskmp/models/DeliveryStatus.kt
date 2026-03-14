package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryStatus {
    PENDING,
    SHIPPED,
    DELIVERED,
    RETURNED
}
