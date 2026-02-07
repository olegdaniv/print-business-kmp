package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BusinessProfile(
    val id: String,
    val ownerName: String,
    val taxId: String,
    val address: String,
    val iban: String,
    val bankName: String,
    val taxPercent: Double,
    val notes: String? = null,
    val updatedAt: Instant
)

@Serializable
data class BusinessProfileUpsertRequest(
    val ownerName: String,
    val taxId: String,
    val address: String,
    val iban: String,
    val bankName: String,
    val taxPercent: Double,
    val notes: String? = null
)
