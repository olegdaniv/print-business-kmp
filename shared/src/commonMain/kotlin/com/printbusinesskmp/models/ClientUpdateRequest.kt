package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class ClientUpdateRequest(
    val name: String,
    val phone: String,
    val email: String? = null
)