package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Invoice(
    val id: String,
    val number: String,
    val orderId: String,
    val issuedAt: Instant,
    val seller: InvoiceSellerSnapshot,
    val client: InvoiceClientSnapshot,
    val lines: List<InvoiceLine>,
    val subtotal: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val notes: String? = null,
    val filePath: String? = null,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0
)

@Serializable
data class InvoiceLine(
    val lineNumber: Int,
    val description: String,
    val quantity: Int,
    val unit: String = "шт",
    val usedMeters: Double,
    val unitPrice: Double,
    val lineTotal: Double
)

@Serializable
data class InvoiceSellerSnapshot(
    val ownerName: String,
    val taxId: String,
    val address: String,
    val iban: String,
    val bankName: String,
    val taxPercent: Double,
    val taxNote: String? = null,
    val mfo: String? = null,
    val ipn: String? = null,
)

@Serializable
data class InvoiceClientSnapshot(
    val type: ClientType,
    val name: String,
    val address: String,
    val phone: String,
    val email: String? = null
)
