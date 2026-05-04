package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object InvoicesTable : Table("invoices") {
    val id = varchar("id", 36)
    val number = varchar("number", 32).uniqueIndex()
    val orderId = optReference(
        name = "order_id",
        refColumn = OrdersTable.id,
        onDelete = ReferenceOption.SET_NULL
    )
    val clientId = varchar("client_id", 36).nullable()
    val issuedAt = timestamp("issued_at").clientDefault { Instant.now() }
    val validUntil = timestamp("valid_until").nullable()
    val payer = varchar("payer", 255).default("той самий")
    val orderRef = varchar("order_ref", 255).default("Без замовлення")

    val sellerOwnerName = varchar("seller_owner_name", 255)
    val sellerTaxId = varchar("seller_tax_id", 50)
    val sellerAddress = varchar("seller_address", 500)
    val sellerIban = varchar("seller_iban", 64)
    val sellerBankName = varchar("seller_bank_name", 255)
    val sellerTaxPercent = double("seller_tax_percent")
    val sellerTaxNote = text("seller_tax_note").nullable()
    val sellerMfo = varchar("seller_mfo", 10).nullable()
    val sellerIpn = varchar("seller_ipn", 20).nullable()

    val clientType = varchar("client_type", 20)
    val clientName = varchar("client_name", 255)
    val clientAddress = varchar("client_address", 500)
    val clientPhone = varchar("client_phone", 50)
    val clientEmail = varchar("client_email", 255).nullable()

    val subtotal = double("subtotal")
    val discountAmount = double("discount_amount").default(0.0)
    val taxAmount = double("tax_amount")
    val totalAmount = double("total_amount")
    val notes = text("notes").nullable()
    val filePath = varchar("file_path", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}