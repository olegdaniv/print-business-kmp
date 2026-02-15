package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object AllowedEmailsTable : Table("allowed_emails") {
    val id = varchar("id", 36)
    val email = varchar("email", 320).uniqueIndex("ux_allowed_emails_email")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val note = varchar("note", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
