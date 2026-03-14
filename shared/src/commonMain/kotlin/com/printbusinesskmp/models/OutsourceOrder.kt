package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class OutsourceOrder(
    val id: String = "",
    val partnerId: String = "",
    val partnerName: String? = null,
    val orderIds: List<String> = emptyList(),
    val serviceType: ServiceType = ServiceType.DTF,
    val description: String = "",
    val specifications: String? = null,
    val totalArea: Double = 0.0,
    val areaUnit: String = "м²",
    val status: OutsourceOrderStatus = OutsourceOrderStatus.PENDING,
    val costFromPartner: Double = 0.0,
    val orderedAt: Instant? = null,
    val expectedDelivery: Instant? = null,
    val receivedAt: Instant? = null,
    val qualityNotes: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class OutsourceOrderCreateRequest(
    val partnerId: String,
    val orderIds: List<String> = emptyList(),
    val serviceType: ServiceType = ServiceType.DTF,
    val description: String,
    val specifications: String? = null,
    val totalArea: Double = 0.0,
    val areaUnit: String = "м²",
    val costFromPartner: Double = 0.0,
    val expectedDelivery: Instant? = null,
)
