package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object OrdersTable : Table("orders") {
    val id = varchar("id", 36)
    val clientId = varchar("client_id", 36)
        .references(ClientsTable.id, onDelete = ReferenceOption.RESTRICT)
    val status = varchar("status", 50)
    val paymentStatus = varchar("payment_status", 50)
    val totalCost = double("total_cost")
    val totalPrice = double("total_price")
    val profit = double("profit")
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
