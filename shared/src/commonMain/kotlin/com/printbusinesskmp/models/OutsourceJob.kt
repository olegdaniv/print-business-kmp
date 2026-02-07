package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class OutsourceJobStatus {
    PLANNED,
    IN_PROGRESS,
    DONE,
    CANCELLED
}

@Serializable
data class OutsourceJob(
    val id: String,
    val orderId: String,
    val partnerId: String,
    val description: String,
    val costToYou: Double,
    val status: OutsourceJobStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class OutsourceJobCreateRequest(
    val orderId: String,
    val partnerId: String,
    val description: String,
    val costToYou: Double,
    val status: OutsourceJobStatus = OutsourceJobStatus.PLANNED
)

@Serializable
data class OutsourceJobUpdateRequest(
    val orderId: String,
    val partnerId: String,
    val description: String,
    val costToYou: Double,
    val status: OutsourceJobStatus
)
