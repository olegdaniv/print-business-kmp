package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

/**
 * Invoice numbering format. [template] is the constant part with trailing zeros
 * defining the digit count, e.g. "СФ-0000000". The sequence number is automatic.
 */
@Serializable
data class InvoiceNumberFormatInfo(
    val template: String,
    val nextNumber: String
)

@Serializable
data class InvoiceNumberFormatUpdateRequest(
    val template: String
)

/** Manual number override for a single invoice. */
@Serializable
data class InvoiceNumberOverrideRequest(
    val number: String
)
