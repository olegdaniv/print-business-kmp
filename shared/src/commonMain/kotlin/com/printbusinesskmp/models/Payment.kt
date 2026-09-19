package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod {
    BANK_TRANSFER,
    CARD,
    CASH,
    COD,
    OTHER
}

/** The part of a [Payment] that settles one order. One payment may cover several orders. */
@Serializable
data class PaymentAllocation(
    val orderId: String,
    val amount: Double
)

/** A single incoming payment from a client (надходження). */
@Serializable
data class Payment(
    val id: String,
    val clientId: String,
    val paidAt: Instant,
    val amount: Double,
    val method: PaymentMethod,
    val purpose: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val allocations: List<PaymentAllocation> = emptyList(),
    val createdAt: Instant
) {
    val allocatedAmount: Double get() = allocations.sumOf { it.amount }

    /** Money received but not yet assigned to any order (client advance). */
    val unallocatedAmount: Double get() = (amount - allocatedAmount).coerceAtLeast(0.0)
}

@Serializable
data class PaymentUpsertRequest(
    val clientId: String,
    val paidAtEpochMs: Long,
    val amount: Double,
    val method: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val purpose: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val allocations: List<PaymentAllocation> = emptyList()
)

/** Marks an invoice as sent to the client; `null` clears the mark. */
@Serializable
data class InvoiceSentRequest(
    val sentAtEpochMs: Long? = null
)

/** Tolerance for comparing money amounts stored as doubles. */
const val MONEY_EPSILON = 0.005
