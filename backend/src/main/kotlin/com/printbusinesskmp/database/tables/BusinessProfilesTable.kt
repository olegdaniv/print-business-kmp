package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object BusinessProfilesTable : Table("business_profiles") {
    val id = varchar("id", 36)
    val ownerName = varchar("owner_name", 255)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 50).nullable()
    val taxId = varchar("tax_id", 50)
    val address = varchar("address", 500)
    val iban = varchar("iban", 64)
    val bankName = varchar("bank_name", 255)
    val taxPercent = double("tax_percent")
    val notes = text("notes").nullable()
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
