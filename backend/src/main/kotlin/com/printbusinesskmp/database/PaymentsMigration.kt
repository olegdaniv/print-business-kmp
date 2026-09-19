package com.printbusinesskmp.database

import com.printbusinesskmp.database.tables.AppSettingsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.database.tables.PaymentAllocationsTable
import com.printbusinesskmp.database.tables.PaymentsTable
import com.printbusinesskmp.models.PaymentMethod
import com.printbusinesskmp.models.PaymentStatus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * One-time conversion of the old manual "Оплачено" flag into real payment records, so
 * the derived payment status of existing orders stays the same. Orders that were marked
 * "Частково" are left without payments (the amount is unknown); they are listed by
 * `GET /api/payments/legacy-partial` for the user to enter manually.
 *
 * Must run inside a transaction.
 */
internal object PaymentsMigration {
    private const val DONE_KEY = "payments_migrated"
    const val MIGRATED_NOTE = "Перенесено з попередньої версії"

    fun migrateLegacyPaidOrders() {
        val done = AppSettingsTable.selectAll()
            .where { AppSettingsTable.settingKey eq DONE_KEY }
            .any()
        if (done) return

        val allocatedOrderIds = PaymentAllocationsTable.selectAll()
            .map { it[PaymentAllocationsTable.orderId] }
            .toSet()

        OrdersTable.selectAll()
            .where { OrdersTable.paymentStatus eq PaymentStatus.PAID.name }
            .filter { it[OrdersTable.id] !in allocatedOrderIds && it[OrdersTable.totalPrice] > 0.0 }
            .forEach { order ->
                val paymentId = UUID.randomUUID().toString()
                PaymentsTable.insert {
                    it[id] = paymentId
                    it[clientId] = order[OrdersTable.clientId]
                    it[paidAt] = order[OrdersTable.updatedAt]
                    it[amount] = order[OrdersTable.totalPrice]
                    it[method] = PaymentMethod.OTHER.name
                    it[notes] = MIGRATED_NOTE
                }
                PaymentAllocationsTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[PaymentAllocationsTable.paymentId] = paymentId
                    it[orderId] = order[OrdersTable.id]
                    it[amount] = order[OrdersTable.totalPrice]
                }
            }

        AppSettingsTable.insert {
            it[settingKey] = DONE_KEY
            it[settingValue] = "true"
        }
    }
}
