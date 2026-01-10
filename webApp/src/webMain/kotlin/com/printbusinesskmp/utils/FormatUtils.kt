package com.printbusinesskmp.utils

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

object FormatUtils {
    /**
     * Formats currency with optional localized suffix.
     * @param amount The amount to format
     * @param currencySuffix Localized currency suffix (e.g., " грн." or " UAH"). Defaults to " UAH".
     * @return Formatted currency string
     */
    fun formatCurrency(amount: Double, currencySuffix: String = " UAH"): String {
        val rounded = (amount * 100).toInt() / 100.0
        val intPart = rounded.toInt()
        val decimalPart = ((rounded - intPart) * 100).toInt()
        return "$intPart.${decimalPart.toString().padStart(2, '0')}$currencySuffix"
    }

    fun formatDate(instant: Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = localDateTime.day.toString().padStart(2, '0')
        val month = localDateTime.month.number.toString().padStart(2, '0')
        return "$day.$month.${localDateTime.year}"
    }

    fun formatDateTime(instant: Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = localDateTime.day.toString().padStart(2, '0')
        val month = localDateTime.month.number.toString().padStart(2, '0')
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        return "$day.$month.${localDateTime.year} $hour:$minute"
    }

    fun formatPhone(phone: String): String {
        // Format Ukrainian phone numbers: +380 XX XXX XX XX
        if (phone.startsWith("+380") && phone.length == 13) {
            return "+380 ${phone.substring(4, 6)} ${phone.substring(6, 9)} ${phone.substring(9, 11)} ${phone.substring(11, 13)}"
        }
        return phone
    }
}