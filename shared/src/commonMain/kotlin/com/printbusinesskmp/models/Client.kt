package com.printbusinesskmp.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val totalOrders: Int = 0,
    val createdAt: Instant
)