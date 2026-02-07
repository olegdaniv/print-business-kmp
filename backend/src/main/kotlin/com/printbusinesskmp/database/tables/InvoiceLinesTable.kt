package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object InvoiceLinesTable : Table("invoice_lines") {
    val id = varchar("id", 36)
    val invoiceId = varchar("invoice_id", 36)
        .references(InvoicesTable.id, onDelete = ReferenceOption.CASCADE)
    val lineNumber = integer("line_number")
    val description = text("description")
    val quantity = integer("quantity")
    val unit = varchar("unit", 20)
    val usedMeters = double("used_meters")
    val unitPrice = double("unit_price")
    val lineTotal = double("line_total")

    override val primaryKey = PrimaryKey(id)
}
