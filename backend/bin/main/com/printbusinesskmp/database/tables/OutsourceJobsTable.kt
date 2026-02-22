package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object OutsourceJobsTable : Table("outsource_jobs") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
        .references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val partnerId = varchar("partner_id", 36)
        .references(PartnersTable.id, onDelete = ReferenceOption.RESTRICT)
    val description = text("description")
    val costToYou = double("cost_to_you")
    val status = varchar("status", 30)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
