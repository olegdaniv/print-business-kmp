package com.printbusinesskmp.utils

import com.printbusinesskmp.models.OrderItemDraft
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.PricingResult
import kotlin.math.max
import kotlin.math.round

object PricingCalculator {

    fun calculate(request: PricingRequest): PricingResult {
        require(request.usedMeters > 0.0) { "Метраж повинен бути більше нуля" }
        require(request.costPerMeter >= 0.0) { "Собівартість метра не може бути від'ємною" }
        require(request.garmentCost >= 0.0) { "Вартість виробу не може бути від'ємною" }
        require(request.overheadPerOrder >= 0.0) { "Накладні витрати не можуть бути від'ємними" }
        require(request.wastePercent >= 0.0) { "Відсоток браку не може бути від'ємним" }
        require(request.setupFee >= 0.0) { "Підготовка не може бути від'ємною" }
        require(request.minOrderPrice > 0.0) { "Мінімальна ціна замовлення повинна бути більше нуля" }

        val materialsCost = (request.usedMeters * request.costPerMeter) + request.garmentCost
        val wasteCost = materialsCost * (request.wastePercent / 100.0)
        val totalCost = materialsCost + wasteCost + request.overheadPerOrder

        val suggestedPrice = max(
            request.minOrderPrice,
            (totalCost * (1 + request.marginPercent / 100.0)) + request.setupFee
        )

        val selectedBasePrice = request.manualPrice
            ?.takeIf { it > 0.0 }
            ?: suggestedPrice

        val normalizedTaxPercent = (request.taxPercent ?: 0.0).coerceAtLeast(0.0)
        val taxAmount = selectedBasePrice * (normalizedTaxPercent / 100.0)
        val finalPrice = selectedBasePrice + taxAmount
        val profit = finalPrice - totalCost

        return PricingResult(
            materialsCost = round2(materialsCost),
            wasteCost = round2(wasteCost),
            overheadCost = round2(request.overheadPerOrder),
            totalCost = round2(totalCost),
            suggestedPrice = round2(suggestedPrice),
            priceBeforeTax = round2(selectedBasePrice),
            taxAmount = round2(taxAmount),
            finalPrice = round2(finalPrice),
            profit = round2(profit)
        )
    }

    fun calculate(item: OrderItemDraft): PricingResult {
        return calculate(
            PricingRequest(
                usedMeters = item.usedMeters,
                costPerMeter = item.pricing.costPerMeter,
                garmentCost = item.garmentCost,
                overheadPerOrder = item.pricing.overheadPerOrder,
                wastePercent = item.pricing.wastePercent,
                setupFee = item.pricing.setupFee,
                minOrderPrice = item.pricing.minOrderPrice,
                marginPercent = item.pricing.marginPercent,
                taxPercent = item.pricing.taxPercent,
                manualPrice = item.manualPrice
            )
        )
    }

    fun formatCurrency(amount: Double): String {
        val rounded = round2(amount)
        val intPart = rounded.toInt()
        val decimalPart = kotlin.math.abs(((rounded - intPart) * 100).toInt())
        return "$intPart.${decimalPart.toString().padStart(2, '0')} грн"
    }

    private fun round2(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}
