package com.printbusinesskmp.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/** Splits one payment across orders: each row settles [amount] of one order. */
object PaymentAllocationsTable : Table("payment_allocations") {
    val id = varchar("id", 36)
    val paymentId = varchar("payment_id", 36)
        .references(PaymentsTable.id, onDelete = ReferenceOption.CASCADE)
    val orderId = varchar("order_id", 36)
        .references(OrdersTable.id, onDelete = ReferenceOption.CASCADE)
    val amount = double("amount")

    override val primaryKey = PrimaryKey(id)
}
