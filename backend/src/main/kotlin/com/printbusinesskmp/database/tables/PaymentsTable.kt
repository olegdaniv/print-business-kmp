package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object PaymentsTable : Table("payments") {
    val id = varchar("id", 36)
    val clientId = varchar("client_id", 36)
        .references(ClientsTable.id, onDelete = ReferenceOption.RESTRICT)
    val paidAt = timestamp("paid_at")
    val amount = double("amount")
    val method = varchar("method", 30)
    val purpose = text("purpose").nullable()
    val reference = varchar("reference", 100).nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
