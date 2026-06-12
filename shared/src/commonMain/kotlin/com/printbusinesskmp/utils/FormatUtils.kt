package com.printbusinesskmp.utils

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

object FormatUtils {
    fun formatCurrency(amount: Double, currencySuffix: String = " UAH"): String {
        val rounded = (amount * 100).toInt() / 100.0
        val intPart = rounded.toInt()
        val decimalPart = kotlin.math.abs(((rounded - intPart) * 100).toInt())
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

    fun formatDecimal(value: Double): String {
        val rounded = (value * 100).toInt() / 100.0
        val intPart = rounded.toInt()
        val decimalPart = kotlin.math.abs(((rounded - intPart) * 100).toInt())
        return "$intPart.${decimalPart.toString().padStart(2, '0')}"
    }

    fun formatPhone(phone: String): String {
        if (phone.startsWith("+380") && phone.length == 13) {
            return "+380 ${phone.substring(4, 6)} ${phone.substring(6, 9)} ${phone.substring(9, 11)} ${phone.substring(11, 13)}"
        }
        return phone
    }

    fun amountInUkrainianWords(amount: Double): String {
        val rounded = kotlin.math.round(amount * 100).toLong()
        val hryvnias = rounded / 100
        val kopecks = (rounded % 100).toInt()
        val hryvniasText = if (hryvnias == 0L) "Нуль" else numberToWords(hryvnias, feminine = true).capitalizeFirst()
        val hryvniasDecl = declineUkrainian(hryvnias, "гривня", "гривні", "гривень")
        val kopecksStr = kopecks.toString().padStart(2, '0')
        return "$hryvniasText $hryvniasDecl $kopecksStr копійок"
    }

    private fun String.capitalizeFirst() = if (isEmpty()) this else this[0].uppercaseChar() + substring(1)

    /** "1 замовлення", "2 замовлення", "5 замовлень" — count with the correct Ukrainian form. */
    fun countUa(n: Int, one: String, few: String, many: String): String =
        "$n ${declineUkrainian(n.toLong(), one, few, many)}"

    private fun declineUkrainian(n: Long, one: String, few: String, many: String): String {
        val lastTwo = (n % 100).toInt()
        val lastOne = (n % 10).toInt()
        return when {
            lastTwo in 11..19 -> many
            lastOne == 1 -> one
            lastOne in 2..4 -> few
            else -> many
        }
    }

    private fun numberToWords(n: Long, feminine: Boolean): String {
        if (n == 0L) return "нуль"
        val parts = mutableListOf<String>()
        val millions = n / 1_000_000
        val thousands = (n % 1_000_000) / 1_000
        val remainder = (n % 1_000).toInt()
        if (millions > 0) {
            parts.add(numberToWords(millions, feminine = false))
            parts.add(declineUkrainian(millions, "мільйон", "мільйони", "мільйонів"))
        }
        if (thousands > 0) {
            parts.add(numberToWords(thousands, feminine = true))
            parts.add(declineUkrainian(thousands, "тисяча", "тисячі", "тисяч"))
        }
        if (remainder > 0) {
            parts.add(threeDigits(remainder, feminine))
        }
        return parts.joinToString(" ")
    }

    private val onesM = arrayOf("", "один", "два", "три", "чотири", "п'ять", "шість", "сім", "вісім", "дев'ять")
    private val onesF = arrayOf("", "одна", "дві", "три", "чотири", "п'ять", "шість", "сім", "вісім", "дев'ять")
    private val teens = arrayOf(
        "десять", "одинадцять", "дванадцять", "тринадцять", "чотирнадцять",
        "п'ятнадцять", "шістнадцять", "сімнадцять", "вісімнадцять", "дев'ятнадцять"
    )
    private val tensArr = arrayOf("", "", "двадцять", "тридцять", "сорок", "п'ятдесят", "шістдесят", "сімдесят", "вісімдесят", "дев'яносто")
    private val hundredsArr = arrayOf("", "сто", "двісті", "триста", "чотириста", "п'ятсот", "шістсот", "сімсот", "вісімсот", "дев'ятсот")

    private fun threeDigits(n: Int, feminine: Boolean): String {
        val parts = mutableListOf<String>()
        if (n >= 100) parts.add(hundredsArr[n / 100])
        val rem = n % 100
        when {
            rem in 10..19 -> parts.add(teens[rem - 10])
            else -> {
                if (rem >= 20) parts.add(tensArr[rem / 10])
                val d = rem % 10
                if (d > 0) parts.add(if (feminine) onesF[d] else onesM[d])
            }
        }
        return parts.filter { it.isNotEmpty() }.joinToString(" ")
    }
}
