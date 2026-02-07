package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class ClientUpdateRequest(
    val type: ClientType,
    val displayName: String,
    val contactName: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String,
    val notes: String? = null
)
