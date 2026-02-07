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
    val address: String,
    val notes: String? = null,
    val orderCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)
