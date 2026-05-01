package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SocialLinks(
    val instagram: String? = null,
    val tiktok: String? = null,
    val facebook: String? = null,
    val telegram: String? = null,
    val website: String? = null,
)

@Serializable
data class BusinessProfile(
    val id: String,
    val ownerName: String,
    val email: String? = null,
    val phone: String? = null,
    val edrpou: String,
    val ipn: String? = null,
    val address: String,
    val iban: String,
    val bankName: String? = null,
    val mfo: String? = null,
    val taxNote: String? = null,
    val certificateNumber: String? = null,
    val taxPercent: Double = 0.0,
    val monthlyEsv: Double = 1900.0,
    val brandName: String? = null,
    val logoUrl: String? = null,
    val socialLinks: SocialLinks? = null,
    val updatedAt: Instant
)

@Serializable
data class BusinessProfileUpsertRequest(
    val ownerName: String,
    val email: String? = null,
    val phone: String? = null,
    val edrpou: String,
    val ipn: String? = null,
    val address: String,
    val iban: String,
    val bankName: String? = null,
    val mfo: String? = null,
    val taxNote: String? = null,
    val certificateNumber: String? = null,
    val taxPercent: Double = 0.0,
    val monthlyEsv: Double = 1900.0,
    val brandName: String? = null,
    val logoUrl: String? = null,
    val socialLinks: SocialLinks? = null
)
