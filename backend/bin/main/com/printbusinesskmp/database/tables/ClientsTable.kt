package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ClientsTable : Table("clients") {
    val id = varchar("id", 36)
    val type = varchar("type", 20)
    val displayName = varchar("display_name", 255)
    val contactName = varchar("contact_name", 255).nullable()
    val phone = varchar("phone", 50)
    val email = varchar("email", 255).nullable()
    val taxId = varchar("tax_id", 50).nullable()
    val address = varchar("address", 500)
    val iban = varchar("iban", 64).nullable()
    val bankName = varchar("bank_name", 255).nullable()
    val notes = text("notes").nullable()
    val deliveryType = varchar("delivery_type", 30).nullable()
    val deliveryCity = varchar("delivery_city", 255).nullable()
    val deliveryBranch = varchar("delivery_branch", 100).nullable()
    val deliveryStreet = varchar("delivery_street", 255).nullable()
    val deliveryBuilding = varchar("delivery_building", 50).nullable()
    val deliveryFreeAddress = varchar("delivery_free_address", 500).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
