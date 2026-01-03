package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String,
    val productType: ProductType,
    val quantity: Int,
    val size: String? = null,
    val color: String? = null,
    val printArea: PrintArea,
    val designUrl: String? = null,
    val blankItemCost: Double,
    val thermalPaperCost: Double,
    val laborCost: Double,
    val totalCost: Double,
    val sellingPrice: Double,
    val profit: Double,
    val notes: String? = null
) {
    companion object {
        fun create(
            id: String,
            productType: ProductType,
            quantity: Int,
            size: String? = null,
            color: String? = null,
            printArea: PrintArea,
            designUrl: String? = null,
            blankItemCost: Double,
            thermalPaperCost: Double,
            laborCost: Double,
            sellingPrice: Double,
            notes: String? = null
        ): OrderItem {
            val totalCost = blankItemCost + thermalPaperCost + laborCost
            val profit = sellingPrice - totalCost

            return OrderItem(
                id = id,
                productType = productType,
                quantity = quantity,
                size = size,
                color = color,
                printArea = printArea,
                designUrl = designUrl,
                blankItemCost = blankItemCost,
                thermalPaperCost = thermalPaperCost,
                laborCost = laborCost,
                totalCost = totalCost,
                sellingPrice = sellingPrice,
                profit = profit,
                notes = notes
            )
        }
    }
}