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
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
