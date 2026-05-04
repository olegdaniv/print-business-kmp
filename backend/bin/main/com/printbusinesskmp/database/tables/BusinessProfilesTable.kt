package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object BusinessProfilesTable : Table("business_profiles") {
    val id = varchar("id", 36)
    val ownerName = varchar("owner_name", 255)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 50).nullable()
    // DB column kept as "tax_id" for backward compatibility; represents ЄДРПОУ
    val edrpou = varchar("tax_id", 50)
    val ipn = varchar("ipn", 20).nullable()
    val address = varchar("address", 500)
    val iban = varchar("iban", 64)
    // Kept non-null in DB; empty string used when optional value is absent
    val bankName = varchar("bank_name", 255)
    val mfo = varchar("mfo", 10).nullable()
    // DB column "notes" repurposed as taxNote
    val taxNote = text("notes").nullable()
    val certificateNumber = varchar("certificate_number", 255).nullable()
    val taxPercent = double("tax_percent")
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
