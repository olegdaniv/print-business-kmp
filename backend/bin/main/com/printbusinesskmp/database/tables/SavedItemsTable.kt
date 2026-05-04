package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SavedItemsTable : Table("saved_items") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()
    val unit = varchar("unit", 20)
    val defaultPrice = double("default_price")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
