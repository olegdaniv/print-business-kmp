package com.printbusinesskmp.utils

import com.printbusinesskmp.models.*

object PricingCalculator {

    fun calculateCostBreakdown(
        productType: ProductType,
        quantity: Int,
        printArea: PrintArea,
        laborMinutes: Int,
        profitMarginPercent: Double,
        materialCosts: MaterialCosts = MaterialCosts(),
        laborRatePerHour: Double = 100.0,
        taxInfo: TaxInfo = TaxInfo()
    ): CostBreakdown {

        val blankItemCost = when(productType) {
            ProductType.T_SHIRT -> materialCosts.blankTShirt
            ProductType.HOODIE -> materialCosts.blankHoodie
            ProductType.CAP -> materialCosts.blankCap
            ProductType.BAG -> materialCosts.blankBag
            ProductType.CUSTOM -> 0.0
        }

        val sheetsNeeded = when(printArea) {
            PrintArea.FRONT, PrintArea.BACK, PrintArea.SLEEVE -> 1
            PrintArea.BOTH -> 2
            PrintArea.CUSTOM -> 1
        }

        val thermalPaperCost = materialCosts.thermalPaperPerSheet * sheetsNeeded
        val totalMaterialsCost = (blankItemCost + thermalPaperCost) * quantity
        val laborCost = (laborMinutes / 60.0) * laborRatePerHour
        val totalCost = totalMaterialsCost + laborCost

        val margin = profitMarginPercent / 100.0
        val sellingPriceBeforeTax = totalCost / (1 - margin - taxInfo.simplifiedTaxRate)
        val tax = sellingPriceBeforeTax * taxInfo.simplifiedTaxRate
        val finalPrice = sellingPriceBeforeTax + tax
        val profit = finalPrice - totalCost - tax

        return CostBreakdown(
            materialsCost = totalMaterialsCost,
            laborCost = laborCost,
            overheadCost = 0.0,
            totalCost = totalCost,
            desiredProfitMargin = margin,
            sellingPriceBeforeTax = sellingPriceBeforeTax,
            simplifiedTax = tax,
            finalSellingPrice = finalPrice,
            actualProfit = profit,
            profitMarginPercent = profitMarginPercent
        )
    }

    fun formatCurrency(amount: Double): String {
        val rounded = (amount * 100).toInt() / 100.0
        val intPart = rounded.toInt()
        val decimalPart = ((rounded - intPart) * 100).toInt()
        return "${intPart}.${decimalPart.toString().padStart(2, '0')} ₴"
    }
}