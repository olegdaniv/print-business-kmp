package com.printbusinesskmp.utils

import com.printbusinesskmp.models.*

object PricingCalculator {

    fun calculateCost(
        productType: ProductType,
        quantity: Int,
        printArea: PrintArea,
        laborMinutes: Int,
        materialCosts: MaterialCosts = MaterialCosts(),
        laborRatePerHour: Double = 100.0  // UAH per hour
    ): CostBreakdown {
        // Calculate blank item cost
        val blankItemCost = when(productType) {
            ProductType.T_SHIRT -> materialCosts.blankTShirt
            ProductType.HOODIE -> materialCosts.blankHoodie
            ProductType.CAP -> materialCosts.blankCap
            ProductType.BAG -> materialCosts.blankBag
            ProductType.CUSTOM -> 0.0
        }

        // Calculate thermal paper cost based on print area
        val sheetsNeeded = when(printArea) {
            PrintArea.FRONT, PrintArea.BACK -> 1
            PrintArea.BOTH -> 2
            PrintArea.SLEEVE -> 1
            PrintArea.CUSTOM -> 1
        }
        val thermalPaperCost = materialCosts.thermalPaperPerSheet * sheetsNeeded

        // Calculate labor cost
        val laborCost = (laborMinutes / 60.0) * laborRatePerHour

        // Total materials
        val totalMaterialsCost = (blankItemCost + thermalPaperCost) * quantity

        // Total cost
        val totalCost = totalMaterialsCost + laborCost

        return CostBreakdown(
            materialsCost = totalMaterialsCost,
            laborCost = laborCost,
            overheadCost = 0.0,
            totalCost = totalCost,
            desiredProfitMargin = 0.0,
            sellingPriceBeforeTax = 0.0,
            simplifiedTax = 0.0,
            finalSellingPrice = 0.0,
            actualProfit = 0.0
        )
    }

    fun calculateSellingPrice(
        costBreakdown: CostBreakdown,
        profitMarginPercent: Double, // e.g., 40 for 40%
        taxInfo: TaxInfo = TaxInfo()
    ): CostBreakdown {
        val margin = profitMarginPercent / 100.0

        // Calculate selling price to achieve desired margin
        // Formula: sellingPrice = totalCost / (1 - margin - taxRate)
        val sellingPriceBeforeTax = costBreakdown.totalCost / (1 - margin - taxInfo.simplifiedTaxRate)

        // Calculate tax
        val tax = sellingPriceBeforeTax * taxInfo.simplifiedTaxRate

        // Final price
        val finalPrice = sellingPriceBeforeTax + tax

        // Actual profit
        val profit = finalPrice - costBreakdown.totalCost - tax

        return costBreakdown.copy(
            desiredProfitMargin = margin,
            sellingPriceBeforeTax = sellingPriceBeforeTax,
            simplifiedTax = tax,
            finalSellingPrice = finalPrice,
            actualProfit = profit
        )
    }

    fun calculateFullPricing(
        productType: ProductType,
        quantity: Int,
        printArea: PrintArea,
        laborMinutes: Int,
        profitMarginPercent: Double,
        materialCosts: MaterialCosts = MaterialCosts(),
        laborRatePerHour: Double = 100.0,
        taxInfo: TaxInfo = TaxInfo()
    ): PricingResponse {
        val costBreakdown = calculateCost(
            productType = productType,
            quantity = quantity,
            printArea = printArea,
            laborMinutes = laborMinutes,
            materialCosts = materialCosts,
            laborRatePerHour = laborRatePerHour
        )

        val finalBreakdown = calculateSellingPrice(
            costBreakdown = costBreakdown,
            profitMarginPercent = profitMarginPercent,
            taxInfo = taxInfo
        )

        return PricingResponse(
            costBreakdown = finalBreakdown,
            perItemPrice = finalBreakdown.finalSellingPrice / quantity,
            totalPrice = finalBreakdown.finalSellingPrice
        )
    }
}