package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    NEW,
    IN_PROGRESS,
    READY,
    COMPLETED,
    CANCELLED
}