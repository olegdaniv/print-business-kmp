package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PartnerType {
    // Legacy values (backward compat)
    PERSON,
    COMPANY,

    // New business-specific values
    DTF_PRINTER,
    UV_DTF_PRINTER,
    BLANK_SUPPLIER,
    SOUVENIR_SUPPLIER,
    DESIGNER,
    OTHER
}

@Serializable
data class PartnerPriceList(
    val dtfPricePerMeter: Double? = null,
    val uvDtfPricePerSheet: Double? = null,
    val uvDtfSheetSize: String? = null,
    val uvDtfPricePerSqMeter: Double? = null,
    val customItems: Map<String, Double> = emptyMap(),
    val updatedAt: Instant? = null,
)

@Serializable
data class Partner(
    val id: String,
    val type: PartnerType,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val services: List<String> = emptyList(),
    val notes: String? = null,
    val website: String? = null,
    val priceList: PartnerPriceList? = null,
    val minimumOrder: String? = null,
    val avgLeadTimeDays: Int? = null,
    val qualityRating: Double? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class PartnerCreateRequest(
    val type: PartnerType,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val services: List<String> = emptyList(),
    val notes: String? = null,
    val website: String? = null,
    val priceList: PartnerPriceList? = null,
    val minimumOrder: String? = null,
    val avgLeadTimeDays: Int? = null,
    val qualityRating: Double? = null
)

@Serializable
data class PartnerUpdateRequest(
    val type: PartnerType,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val services: List<String> = emptyList(),
    val notes: String? = null,
    val website: String? = null,
    val priceList: PartnerPriceList? = null,
    val minimumOrder: String? = null,
    val avgLeadTimeDays: Int? = null,
    val qualityRating: Double? = null,
    val isActive: Boolean? = null
)
