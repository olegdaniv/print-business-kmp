package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class PricingRequest(
    val usedMeters: Double,
    val costPerMeter: Double,
    val garmentCost: Double,
    val overheadPerOrder: Double,
    val wastePercent: Double,
    val setupFee: Double,
    val minOrderPrice: Double,
    val marginPercent: Double,
    val taxPercent: Double? = null,
    val manualPrice: Double? = null
)

@Serializable
data class PricingResult(
    val materialsCost: Double,
    val wasteCost: Double,
    val overheadCost: Double,
    val totalCost: Double,
    val suggestedPrice: Double,
    val priceBeforeTax: Double,
    val taxAmount: Double,
    val finalPrice: Double,
    val profit: Double
)
