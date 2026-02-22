package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36)
        .references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val serviceType = varchar("service_type", 20)
    val productType = varchar("product_type", 20)
    val quantity = integer("quantity")
    val usedMeters = double("used_meters")
    val garmentCost = double("garment_cost")

    val costPerMeter = double("cost_per_meter")
    val overheadPerOrder = double("overhead_per_order")
    val wastePercent = double("waste_percent")
    val setupFee = double("setup_fee")
    val minOrderPrice = double("min_order_price")
    val marginPercent = double("margin_percent")
    val taxPercent = double("tax_percent").nullable()

    val manualPrice = double("manual_price").nullable()
    val cost = double("cost")
    val price = double("price")
    val taxAmount = double("tax_amount")
    val profit = double("profit")
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(id)
}
