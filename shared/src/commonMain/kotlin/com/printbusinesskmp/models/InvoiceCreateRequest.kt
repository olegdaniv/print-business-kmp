package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceCreateRequest(
    val clientId: String,
    val lines: List<InvoiceLineRequest>,
    val discountAmount: Double = 0.0,
    val notes: String? = null,
    val payer: String = "той самий",
    val orderRef: String = "Без замовлення",
    val validDays: Int = 7
)

@Serializable
data class InvoiceLineRequest(
    val description: String,
    val unit: String = "шт.",
    val quantity: Int,
    val unitPrice: Double
)