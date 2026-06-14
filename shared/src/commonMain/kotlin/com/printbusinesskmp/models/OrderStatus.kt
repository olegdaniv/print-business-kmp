package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Suppress("DEPRECATION")
@Serializable
enum class OrderStatus {
DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    IN_PRODUCTION,
    QUALITY_CHECK,
    READY,
    SHIPPED,
    COMPLETED,
    OUTSOURCE_ORDERED,
    OUTSOURCE_RECEIVED,
    CANCELLED,

    // Backward compat — maps to DRAFT
    @Deprecated("Use DRAFT instead", replaceWith = ReplaceWith("DRAFT"))
    NEW;

    val isCancellable: Boolean
        get() = this in setOf(DRAFT, PENDING_APPROVAL, APPROVED, NEW)

    val isEditable: Boolean
        get() = this in setOf(DRAFT, PENDING_APPROVAL, NEW)

    val isFinal: Boolean
        get() = this in setOf(COMPLETED, CANCELLED)

    val allowedTransitions: Set<OrderStatus>
        get() = when (this) {
            DRAFT, NEW -> setOf(PENDING_APPROVAL, APPROVED, CANCELLED)
            PENDING_APPROVAL -> setOf(APPROVED, CANCELLED)
            APPROVED -> setOf(OUTSOURCE_ORDERED, IN_PRODUCTION, CANCELLED)
            OUTSOURCE_ORDERED -> setOf(OUTSOURCE_RECEIVED, CANCELLED)
            OUTSOURCE_RECEIVED -> setOf(IN_PRODUCTION)
            IN_PRODUCTION -> setOf(QUALITY_CHECK, READY)
            QUALITY_CHECK -> setOf(READY, IN_PRODUCTION)
            READY -> setOf(SHIPPED, COMPLETED)
            SHIPPED -> setOf(COMPLETED)
            COMPLETED -> emptySet()
            CANCELLED -> emptySet()
        }
}

@Serializable
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID
}