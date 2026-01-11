package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table

object InvoiceItemsTable : Table("invoice_items") {
    val id = varchar("id", 36)
    val invoiceId = varchar("invoice_id", 36).references(InvoicesTable.id)
    val itemNumber = integer("item_number")
    val description = text("description")
    val quantity = integer("quantity")
    val unit = varchar("unit", 20).default("шт.")
    val pricePerUnit = double("price_per_unit")
    val totalPrice = double("total_price")

    override val primaryKey = PrimaryKey(id)
}
