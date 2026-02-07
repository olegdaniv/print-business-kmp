package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PartnerType {
    PERSON,
    COMPANY
}

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
    val notes: String? = null
)

@Serializable
data class PartnerUpdateRequest(
    val type: PartnerType,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val services: List<String> = emptyList(),
    val notes: String? = null
)
