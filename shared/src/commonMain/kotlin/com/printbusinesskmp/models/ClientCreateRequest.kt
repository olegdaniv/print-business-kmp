package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class ClientCreateRequest(
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
    val source: ClientSource? = null,
    val discountPercent: Double? = null
)
