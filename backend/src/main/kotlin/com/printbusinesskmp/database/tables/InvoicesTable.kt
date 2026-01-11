package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object InvoicesTable : Table("invoices") {
    val id = varchar("id", 36)
    val number = integer("number").autoIncrement()
    val date = timestamp("date")
    val orderId = varchar("order_id", 36).references(OrdersTable.id)
    val clientName = varchar("client_name", 255)
    val clientPhone = varchar("client_phone", 50)
    val clientEmail = varchar("client_email", 255).nullable()
    val clientAddress = varchar("client_address", 500).nullable()
    val totalAmount = double("total_amount")
    val notes = text("notes").nullable()
    val generatedAt = timestamp("generated_at").clientDefault { Instant.now() }
    val filePath = varchar("file_path", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
