package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class CostBreakdown(
    val materialsCost: Double,           // Blank item + thermal paper
    val laborCost: Double,               // Your work time
    val overheadCost: Double = 0.0,      // Optional: electricity, rent portion
    val totalCost: Double,               // Sum of above
    val desiredProfitMargin: Double,     // 0.40 for 40%, 0.65 for 65%
    val sellingPriceBeforeTax: Double,   // totalCost / (1 - margin)
    val simplifiedTax: Double,           // 5% of selling price
    val finalSellingPrice: Double,       // sellingPrice + tax
    val actualProfit: Double             // finalSellingPrice - totalCost - tax
)

@Serializable
data class MaterialCosts(
    val thermalPaperPerSheet: Double = 5.0,    // UAH per A4 sheet
    val blankTShirt: Double = 150.0,           // UAH
    val blankHoodie: Double = 400.0,           // UAH
    val blankCap: Double = 80.0,               // UAH
    val blankBag: Double = 100.0               // UAH
)

@Serializable
data class TaxInfo(
    val simplifiedTaxRate: Double = 0.05,      // 5% group 3
    val monthlyESV: Double = 1474.0,           // 2026 rate (update annually)
    val monthlyLimit: Double = 1167000.0       // Group 3 limit
)

@Serializable
data class PricingRequest(
    val productType: ProductType,
    val quantity: Int,
    val printArea: PrintArea,
    val laborMinutes: Int,
    val profitMarginPercent: Double,
    val materialCosts: MaterialCosts = MaterialCosts(),
    val laborRatePerHour: Double = 100.0
)

@Serializable
data class PricingResponse(
    val costBreakdown: CostBreakdown,
    val perItemPrice: Double,
    val totalPrice: Double
)