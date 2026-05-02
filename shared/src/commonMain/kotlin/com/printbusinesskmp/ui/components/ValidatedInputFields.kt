package com.printbusinesskmp.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Inserts a space separator between fixed-size groups of characters.
 * Groups are processed left-to-right; any remainder beyond the defined groups is appended as-is.
 *
 * Example: groups=[3,3,4], raw="0501234567" → "050 123 4567"
 */
private class GroupSeparatorTransformation(private val groups: List<Int>) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = buildString {
            var pos = 0
            for ((i, size) in groups.withIndex()) {
                if (pos >= raw.length) break
                if (i > 0) append(' ')
                val end = minOf(pos + size, raw.length)
                append(raw.substring(pos, end))
                pos += size
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var result = 0
                var rem = offset
                for ((i, size) in groups.withIndex()) {
                    if (i > 0 && rem > 0) result++ // separator
                    val take = minOf(rem, size)
                    result += take
                    rem -= take
                    if (rem <= 0) break
                }
                result += rem // any remainder beyond groups
                return result
            }

            override fun transformedToOriginal(offset: Int): Int {
                var result = 0
                var rem = offset
                for ((i, size) in groups.withIndex()) {
                    if (i > 0) {
                        if (rem <= 0) break
                        rem-- // consume separator
                    }
                    val take = minOf(rem, size)
                    result += take
                    rem -= take
                    if (rem <= 0) break
                }
                return result
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ── Phone ───────────────────────────────────────────────────────────────────

/**
 * Ukrainian phone number field: exactly 10 digits starting with 0.
 * Displays as "0XX XXX XXXX"; accepts only digits.
 * Value is stored/returned as raw digits (no spaces).
 */
@Composable
fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Телефон",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 10
    val inlineError = value.length == maxLen && !value.startsWith("0")
    val showError = isError || inlineError
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        placeholder = { Text("0XX XXX XXXX") },
        isError = showError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                inlineError -> Text("Номер має починатися з 0", color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        visualTransformation = GroupSeparatorTransformation(listOf(3, 3, 4)),
        modifier = modifier
    )
}

// ── IBAN ────────────────────────────────────────────────────────────────────

/**
 * Ukrainian IBAN field: "UA" prefix + exactly 27 digits = 29 characters.
 * Displays with spaces every 4 chars for readability.
 * Value is stored/returned without spaces (e.g. "UA853996220000026001233566 1" → no spaces).
 */
@Composable
fun IbanField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "IBAN",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 29
    // Groups of 4, last group has 1 char: 4*7 + 1 = 29
    val groups = listOf(4, 4, 4, 4, 4, 4, 4, 1)
    val inlineError = value.isNotEmpty() && !value.startsWith("UA")
    val showError = isError || inlineError
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val clean = raw.replace(" ", "").uppercase()
            val result = StringBuilder()
            var digitCount = 0
            for (c in clean) {
                when {
                    result.length == 0 && c == 'U' -> result.append(c)
                    result.length == 1 && c == 'A' -> result.append(c)
                    result.length >= 2 && c.isDigit() && digitCount < 27 -> {
                        result.append(c)
                        digitCount++
                    }
                }
                if (result.length >= maxLen) break
            }
            onValueChange(result.toString())
        },
        label = { Text(label) },
        placeholder = { Text("UA + 27 цифр") },
        isError = showError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                inlineError -> Text("Має починатися з UA", color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        visualTransformation = GroupSeparatorTransformation(groups),
        modifier = modifier
    )
}

// ── Card Number ─────────────────────────────────────────────────────────────

/**
 * 16-digit card number field (optional). Displays as "XXXX XXXX XXXX XXXX".
 * Value is stored/returned as raw digits (no spaces).
 */
@Composable
fun CardNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Номер картки",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 16
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        placeholder = { Text("XXXX XXXX XXXX XXXX") },
        isError = isError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        visualTransformation = GroupSeparatorTransformation(listOf(4, 4, 4, 4)),
        modifier = modifier
    )
}

// ── EDRPOU ──────────────────────────────────────────────────────────────────

/**
 * ЄДРПОУ field: exactly 8 digits.
 * Value is stored/returned as raw digits.
 */
@Composable
fun EdrpouField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ЄДРПОУ",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 8
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        placeholder = { Text("8 цифр") },
        isError = isError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        modifier = modifier
    )
}

// ── IPN ─────────────────────────────────────────────────────────────────────

/**
 * ІПН / РНОКПП field: exactly 10 digits.
 * Value is stored/returned as raw digits.
 */
@Composable
fun IpnField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ІПН",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 10
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        placeholder = { Text("10 цифр") },
        isError = isError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        modifier = modifier
    )
}

// ── MFO ─────────────────────────────────────────────────────────────────────

/**
 * МФО field: exactly 6 digits.
 * Value is stored/returned as raw digits.
 */
@Composable
fun MfoField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "МФО",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val maxLen = 6
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        placeholder = { Text("6 цифр") },
        isError = isError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(errorMessage, color = Color.Red)
                else -> Text("${value.length}/$maxLen")
            }
        },
        modifier = modifier
    )
}