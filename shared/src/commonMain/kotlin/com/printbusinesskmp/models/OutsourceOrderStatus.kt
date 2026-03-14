package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class OutsourceOrderStatus {
    PENDING,
    SENT,
    IN_PRODUCTION,
    READY,
    RECEIVED,
    QUALITY_CHECK,
    ACCEPTED,
    REJECTED
}
