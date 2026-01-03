package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ClientsTable : Table("clients") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val phone = varchar("phone", 50)
    val email = varchar("email", 255).nullable()
    val totalOrders = integer("total_orders").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}