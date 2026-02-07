package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object PartnersTable : Table("partners") {
    val id = varchar("id", 36)
    val type = varchar("type", 20)
    val name = varchar("name", 255)
    val phone = varchar("phone", 50).nullable()
    val email = varchar("email", 255).nullable()
    val address = varchar("address", 500).nullable()
    val services = text("services_json")
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
