package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.Table

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36).references(OrdersTable.id)
    val productType = varchar("product_type", 50)
    val quantity = integer("quantity")
    val size = varchar("size", 20).nullable()
    val color = varchar("color", 50).nullable()
    val printArea = varchar("print_area", 50)
    val designUrl = text("design_url").nullable()
    val blankItemCost = double("blank_item_cost")
    val thermalPaperCost = double("thermal_paper_cost")
    val laborCost = double("labor_cost")
    val totalCost = double("total_cost")
    val sellingPrice = double("selling_price")
    val profit = double("profit")
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(id)
}