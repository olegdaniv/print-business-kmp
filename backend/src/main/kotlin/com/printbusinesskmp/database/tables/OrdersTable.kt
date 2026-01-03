package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object OrdersTable : Table("orders") {
    val id = varchar("id", 36)
    val clientId = varchar("client_id", 36).references(ClientsTable.id)
    val status = varchar("status", 50)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val completedAt = timestamp("completed_at").nullable()
    val totalCost = double("total_cost")
    val totalPrice = double("total_price")
    val totalProfit = double("total_profit")
    val notes = text("notes").nullable()
    val invoiceGenerated = bool("invoice_generated").default(false)

    override val primaryKey = PrimaryKey(id)
}