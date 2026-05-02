package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ClientType {
    PERSON,
    COMPANY
}

@Serializable
enum class DeliveryType {
    NOVA_POSHTA_BRANCH,   // Відділення Нової Пошти
    NOVA_POSHTA_ADDRESS,  // Адресна доставка НП
    DIRECT_ADDRESS        // Пряма адреса
}

@Serializable
data class ClientDelivery(
    val type: DeliveryType,
    val city: String? = null,         // NOVA_POSHTA_BRANCH, NOVA_POSHTA_ADDRESS
    val branch: String? = null,       // NOVA_POSHTA_BRANCH  e.g. "Відділення №5"
    val street: String? = null,       // NOVA_POSHTA_ADDRESS
    val building: String? = null,     // NOVA_POSHTA_ADDRESS
    val freeAddress: String? = null   // DIRECT_ADDRESS
) {
    fun label(): String = when (type) {
        DeliveryType.NOVA_POSHTA_BRANCH -> buildString {
            if (!city.isNullOrBlank()) append(city)
            if (!branch.isNullOrBlank()) { if (isNotEmpty()) append(", "); append(branch) }
            if (isEmpty()) append("НП Відділення")
        }
        DeliveryType.NOVA_POSHTA_ADDRESS -> buildString {
            if (!city.isNullOrBlank()) append(city)
            if (!street.isNullOrBlank()) { if (isNotEmpty()) append(", "); append(street) }
            if (!building.isNullOrBlank()) { if (isNotEmpty()) append(" "); append(building) }
            if (isEmpty()) append("НП Адресна")
        }
        DeliveryType.DIRECT_ADDRESS -> freeAddress?.trim()?.takeIf { it.isNotEmpty() } ?: "Пряма адреса"
    }
}

@Serializable
data class Client(
    val id: String,
    val type: ClientType,
    val displayName: String,
    val contactName: String? = null,
    val phone: String,
    val email: String? = null,
    val taxId: String? = null,
    val address: String,
    val iban: String? = null,
    val bankName: String? = null,
    val notes: String? = null,
    val orderCount: Int = 0,
    val source: ClientSource? = null,
    val discountPercent: Double? = null,
    val delivery: ClientDelivery? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
