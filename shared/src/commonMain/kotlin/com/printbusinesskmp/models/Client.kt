package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val totalOrders: Int = 0,
    @Contextual val createdAt: Instant
)