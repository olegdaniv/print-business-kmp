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
    val manualPrice: Double? = null,
    val garmentSource: GarmentSource = GarmentSource.OUR_STOCK,
    val partnerPrintCostFlat: Double = 0.0,
    val laborCostPerUnit: Double = 0.0,
    val serviceType: ServiceType = ServiceType.DTF,
    val productType: ProductType = ProductType.T_SHIRT
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
    val profit: Double,
    val partnerPrintCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val actualMarginPercent: Double = 0.0
)
