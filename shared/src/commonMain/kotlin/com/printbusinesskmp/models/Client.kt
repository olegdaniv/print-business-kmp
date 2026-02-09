package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ClientType {
    PERSON,
    COMPANY
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
    val createdAt: Instant,
    val updatedAt: Instant
)
