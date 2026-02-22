package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object LayoutsTable : Table("layouts") {
    val id = varchar("id", 36)
    val clientId = optReference(
        name = "client_id",
        refColumn = ClientsTable.id,
        onDelete = ReferenceOption.SET_NULL
    )
    val name = varchar("name", 255)
    val serviceType = varchar("service_type", 20)
    val status = varchar("status", 30)
    val widthCm = double("width_cm")
    val heightCm = double("height_cm")
    val dpi = integer("dpi")
    val previewUrl = text("preview_url").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
