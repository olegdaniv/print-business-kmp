package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class PricingConfig(
    val costPerMeter: Double,
    val overheadPerOrder: Double,
    val wastePercent: Double,
    val setupFee: Double,
    val minOrderPrice: Double,
    val marginPercent: Double,
    val taxPercent: Double? = null,
    val partnerPrintCostFlat: Double = 0.0,
    val laborCostPerUnit: Double = 0.0
)

@Serializable
data class OrderItemDraft(
    val serviceType: ServiceType,
    val productType: ProductType,
    val quantity: Int,
    val usedMeters: Double,
    val garmentCost: Double,
    val pricing: PricingConfig,
    val manualPrice: Double? = null,
    val notes: String? = null,
    val garmentSource: GarmentSource = GarmentSource.OUR_STOCK,
    val size: String? = null,
    val color: String? = null,
    val printCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val designId: String? = null,
    val outsourceOrderId: String? = null
)

@Serializable
data class OrderItem(
    val id: String,
    val serviceType: ServiceType,
    val productType: ProductType,
    val quantity: Int,
    val usedMeters: Double,
    val garmentCost: Double,
    val pricing: PricingConfig,
    val manualPrice: Double? = null,
    val cost: Double,
    val price: Double,
    val taxAmount: Double,
    val profit: Double,
    val notes: String? = null,
    val garmentSource: GarmentSource = GarmentSource.OUR_STOCK,
    val size: String? = null,
    val color: String? = null,
    val printCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val designId: String? = null,
    val outsourceOrderId: String? = null
)
