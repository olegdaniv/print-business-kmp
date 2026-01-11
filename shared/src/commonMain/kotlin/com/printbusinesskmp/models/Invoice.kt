package com.printbusinesskmp.models

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Invoice(
    val id: String,
    val number: Int,
    val date: Instant,
    val orderId: String,
    val client: InvoiceClient,
    val items: List<InvoiceItem>,
    val totalAmount: Double,
    val notes: String? = null,
    val generatedAt: Instant,
    val filePath: String? = null
)

@Serializable
data class InvoiceClient(
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null
)

@Serializable
data class InvoiceItem(
    val number: Int,
    val description: String,  // e.g., "Термодрук на футболці (чорна, розмір L, друк на грудях)"
    val quantity: Int,
    val unit: String = "шт.",
    val pricePerUnit: Double,
    val totalPrice: Double
)

@Serializable
data class FopDetails(
    val fullName: String = "Данів Олег Романович",
    val taxId: String = "3387102533",
    val address: String = "Україна, 80105, Львівська обл., м. Червоноград пр-т Шевченка, буд. 6, кв. 12",
    val iban: String = "UA653510050000026007879179119",
    val bank: String = "АТ «УКРСИББАНК»",
    val mfo: String = "351005",
    val phone: String = "+380636355264",
    val email: String = "oleg.daniv25@gmail.com"
)
