package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.database.tables.PaymentAllocationsTable
import com.printbusinesskmp.database.tables.PaymentsTable
import com.printbusinesskmp.models.MONEY_EPSILON
import com.printbusinesskmp.models.Payment
import com.printbusinesskmp.models.PaymentAllocation
import com.printbusinesskmp.models.PaymentMethod
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PaymentUpsertRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/** Money received for one order: total and the date of the latest payment. */
data class OrderPaymentTotals(val paidAmount: Double, val lastPaidAt: Instant?)

class PaymentRepository {

    suspend fun allPayments(orderId: String? = null): List<Payment> = dbQuery {
        val paymentIds = orderId?.let { id ->
            PaymentAllocationsTable.selectAll()
                .where { PaymentAllocationsTable.orderId eq id }
                .map { it[PaymentAllocationsTable.paymentId] }
                .toSet()
        }
        val rows = PaymentsTable.selectAll()
            .let { query -> paymentIds?.let { ids -> query.where { PaymentsTable.id inList ids } } ?: query }
            .toList()
        val allocations = allocationsFor(rows.map { it[PaymentsTable.id] })
        rows.map { toPayment(it, allocations[it[PaymentsTable.id]].orEmpty()) }
            .sortedByDescending { it.paidAt }
    }

    suspend fun paymentById(id: String): Payment? = dbQuery { findPayment(id) }

    suspend fun addPayment(request: PaymentUpsertRequest): Payment = dbQuery {
        validate(request, excludePaymentId = null)
        val id = UUID.randomUUID().toString()
        PaymentsTable.insert {
            it[PaymentsTable.id] = id
            it[createdAt] = Instant.now()
            writeFields(it, request)
        }
        insertAllocations(id, request.allocations)
        findPayment(id)!!
    }

    suspend fun updatePayment(id: String, request: PaymentUpsertRequest): Payment? = dbQuery {
        if (findPayment(id) == null) return@dbQuery null
        validate(request, excludePaymentId = id)
        PaymentsTable.update({ PaymentsTable.id eq id }) { writeFields(it, request) }
        PaymentAllocationsTable.deleteWhere { paymentId eq id }
        insertAllocations(id, request.allocations)
        findPayment(id)
    }

    suspend fun deletePayment(id: String): Boolean = dbQuery {
        PaymentAllocationsTable.deleteWhere { paymentId eq id }
        PaymentsTable.deleteWhere { PaymentsTable.id eq id } > 0
    }

    /** Orders still carrying the legacy "PARTIAL" flag with no payment recorded yet. */
    suspend fun legacyPartialOrderIds(): List<String> = dbQuery {
        val allocated = PaymentAllocationsTable.selectAll()
            .map { it[PaymentAllocationsTable.orderId] }
            .toSet()
        OrdersTable.selectAll()
            .where { OrdersTable.paymentStatus eq PaymentStatus.PARTIAL.name }
            .map { it[OrdersTable.id] }
            .filter { it !in allocated }
    }

    private fun findPayment(id: String): Payment? {
        val row = PaymentsTable.selectAll().where { PaymentsTable.id eq id }.singleOrNull() ?: return null
        return toPayment(row, allocationsFor(listOf(id))[id].orEmpty())
    }

    private fun validate(request: PaymentUpsertRequest, excludePaymentId: String?) {
        if (request.amount <= 0.0) {
            throw IllegalArgumentException("Сума надходження повинна бути більше нуля")
        }
        val clientExists = ClientsTable.selectAll().where { ClientsTable.id eq request.clientId }.any()
        if (!clientExists) {
            throw IllegalArgumentException("Клієнта не знайдено")
        }
        if (request.allocations.any { it.amount <= 0.0 }) {
            throw IllegalArgumentException("Сума розподілу повинна бути більше нуля")
        }
        val orderIds = request.allocations.map { it.orderId }
        if (orderIds.size != orderIds.toSet().size) {
            throw IllegalArgumentException("Замовлення в розподілі повторюється")
        }
        if (request.allocations.sumOf { it.amount } > request.amount + MONEY_EPSILON) {
            throw IllegalArgumentException("Розподілено більше, ніж сума надходження")
        }
        if (orderIds.isNotEmpty()) {
            val orderClients = OrdersTable.selectAll()
                .where { OrdersTable.id inList orderIds }
                .associate { it[OrdersTable.id] to it[OrdersTable.clientId] }
            orderIds.forEach { orderId ->
                val clientId = orderClients[orderId]
                    ?: throw IllegalArgumentException("Замовлення не знайдено")
                if (clientId != request.clientId) {
                    throw IllegalArgumentException("Замовлення належить іншому клієнту")
                }
            }
        }
    }

    private fun writeFields(
        statement: org.jetbrains.exposed.sql.statements.UpdateBuilder<*>,
        request: PaymentUpsertRequest
    ) {
        statement[PaymentsTable.clientId] = request.clientId
        statement[PaymentsTable.paidAt] = Instant.ofEpochMilli(request.paidAtEpochMs)
        statement[PaymentsTable.amount] = request.amount
        statement[PaymentsTable.method] = request.method.name
        statement[PaymentsTable.purpose] = request.purpose?.trim()?.takeIf { it.isNotEmpty() }
        statement[PaymentsTable.reference] = request.reference?.trim()?.takeIf { it.isNotEmpty() }
        statement[PaymentsTable.notes] = request.notes?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun insertAllocations(paymentId: String, allocations: List<PaymentAllocation>) {
        allocations.forEach { allocation ->
            PaymentAllocationsTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[PaymentAllocationsTable.paymentId] = paymentId
                it[orderId] = allocation.orderId
                it[amount] = allocation.amount
            }
        }
    }

    private fun allocationsFor(paymentIds: List<String>): Map<String, List<PaymentAllocation>> {
        if (paymentIds.isEmpty()) return emptyMap()
        return PaymentAllocationsTable.selectAll()
            .where { PaymentAllocationsTable.paymentId inList paymentIds }
            .groupBy(
                keySelector = { it[PaymentAllocationsTable.paymentId] },
                valueTransform = {
                    PaymentAllocation(
                        orderId = it[PaymentAllocationsTable.orderId],
                        amount = it[PaymentAllocationsTable.amount]
                    )
                }
            )
    }

    private fun toPayment(row: ResultRow, allocations: List<PaymentAllocation>): Payment = Payment(
        id = row[PaymentsTable.id],
        clientId = row[PaymentsTable.clientId],
        paidAt = kotlin.time.Instant.fromEpochMilliseconds(row[PaymentsTable.paidAt].toEpochMilli()),
        amount = row[PaymentsTable.amount],
        method = runCatching { PaymentMethod.valueOf(row[PaymentsTable.method]) }.getOrDefault(PaymentMethod.OTHER),
        purpose = row[PaymentsTable.purpose],
        reference = row[PaymentsTable.reference],
        notes = row[PaymentsTable.notes],
        allocations = allocations,
        createdAt = kotlin.time.Instant.fromEpochMilliseconds(row[PaymentsTable.createdAt].toEpochMilli())
    )

    companion object {
        /**
         * Paid totals per order in one grouped query. Pass [orderIds] to limit the scan;
         * must run inside a transaction.
         */
        fun paymentTotals(orderIds: Collection<String>? = null): Map<String, OrderPaymentTotals> {
            val sumExpr = PaymentAllocationsTable.amount.sum()
            val lastExpr = PaymentsTable.paidAt.max()
            return PaymentAllocationsTable.innerJoin(PaymentsTable)
                .select(PaymentAllocationsTable.orderId, sumExpr, lastExpr)
                .let { query ->
                    orderIds?.let { ids -> query.where { PaymentAllocationsTable.orderId inList ids } } ?: query
                }
                .groupBy(PaymentAllocationsTable.orderId)
                .associate { row ->
                    row[PaymentAllocationsTable.orderId] to OrderPaymentTotals(
                        paidAmount = row[sumExpr] ?: 0.0,
                        lastPaidAt = row[lastExpr]
                    )
                }
        }
    }
}
