package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.MONEY_EPSILON
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.Payment
import com.printbusinesskmp.models.PaymentAllocation
import com.printbusinesskmp.models.PaymentMethod
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PaymentUpsertRequest
import com.printbusinesskmp.ui.components.LabeledDropdown
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.itemsSummary
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Instant

/** Parses a "dd.MM.yyyy" string to epoch milliseconds at local start-of-day, or null. */
internal fun parseUaDateToEpochMs(input: String): Long? {
    val parts = input.trim().split('.')
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return try {
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

/** Accepts "1 250,50" as well as "1250.5". */
internal fun parseMoney(input: String): Double? =
    input.trim().replace(" ", "").replace(' '.toString(), "").replace(',', '.').toDoubleOrNull()

internal fun formatMoneyInput(amount: Double): String = FormatUtils.formatDecimal(amount)

/**
 * An order the client still owes money for. Cancelled orders are never billed; a draft
 * only counts once an invoice has been issued for it.
 */
internal fun Order.isReceivable(hasInvoice: Boolean = false): Boolean =
    status != OrderStatus.CANCELLED && (status != OrderStatus.DRAFT || hasInvoice) && balanceDue > MONEY_EPSILON

private fun Instant.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(ZoneId.systemDefault()).toLocalDate()

internal enum class PaymentPeriod(val label: String) {
    THIS_MONTH("Цей місяць"),
    LAST_MONTH("Минулий місяць"),
    THIS_YEAR("Цей рік");

    fun contains(instant: Instant): Boolean {
        val date = instant.toLocalDate()
        val today = LocalDate.now()
        return when (this) {
            THIS_MONTH -> date.year == today.year && date.month == today.month
            LAST_MONTH -> today.minusMonths(1).let { date.year == it.year && date.month == it.month }
            THIS_YEAR -> date.year == today.year
        }
    }
}

/** Marks an invoice as sent (or clears the mark). */
@Composable
internal fun InvoiceSentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onSaved: (Invoice) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var dateInput by remember { mutableStateOf(FormatUtils.formatDate(invoice.sentAt ?: Clock.System.now())) }
    val parsedMs = parseUaDateToEpochMs(dateInput)

    fun save(ms: Long?) {
        onDismiss()
        scope.launch {
            try {
                onSaved(ApiClient.setInvoiceSent(invoice.id, ms))
            } catch (e: Exception) {
                onError(e.message ?: "Не вдалося зберегти дату надсилання")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Рахунок № ${invoice.number} надіслано") },
        text = {
            Column {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Дата надсилання (дд.мм.рррр)") },
                    singleLine = true,
                    isError = dateInput.isNotBlank() && parsedMs == null
                )
                if (dateInput.isNotBlank() && parsedMs == null) {
                    Text(
                        "Невірний формат. Приклад: 25.06.2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = parsedMs != null, onClick = { save(parsedMs) }) { Text("Зберегти") }
        },
        dismissButton = {
            Row {
                if (invoice.sentAt != null) {
                    TextButton(onClick = { save(null) }) {
                        Text("Зняти позначку", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Скасувати") }
            }
        }
    )
}

/**
 * Create or edit an incoming payment and split it across the client's orders.
 * When opened from an order ([presetOrderId]) that order is filled first.
 */
@Composable
internal fun PaymentEditorDialog(
    existing: Payment?,
    clients: List<Client>,
    orders: List<Order>,
    presetClientId: String? = null,
    presetOrderId: String? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // This payment's own share per order: editing must not count it as already paid.
    val ownShare = remember(existing) { existing?.allocations?.associate { it.orderId to it.amount }.orEmpty() }
    fun dueExcludingThis(order: Order): Double = order.balanceDue + (ownShare[order.id] ?: 0.0)

    var clientId by remember { mutableStateOf(existing?.clientId ?: presetClientId) }
    var dateInput by remember { mutableStateOf(FormatUtils.formatDate(existing?.paidAt ?: Clock.System.now())) }
    var amountInput by remember {
        mutableStateOf(
            existing?.amount?.let(::formatMoneyInput)
                ?: orders.find { it.id == presetOrderId }?.balanceDue?.takeIf { it > MONEY_EPSILON }?.let(::formatMoneyInput)
                ?: ""
        )
    }
    var method by remember { mutableStateOf(existing?.method ?: PaymentMethod.BANK_TRANSFER) }
    var purpose by remember { mutableStateOf(existing?.purpose.orEmpty()) }
    var reference by remember { mutableStateOf(existing?.reference.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var allocInputs by remember {
        mutableStateOf(existing?.allocations?.associate { it.orderId to formatMoneyInput(it.amount) }.orEmpty())
    }
    // Allocations follow the amount until the user edits one by hand.
    var autoAllocate by remember { mutableStateOf(existing == null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val candidates = remember(clientId, orders) {
        orders
            .filter { it.clientId == clientId }
            .filter { order ->
                order.id in ownShare || order.id == presetOrderId ||
                    (order.status != OrderStatus.CANCELLED && dueExcludingThis(order) > MONEY_EPSILON)
            }
            .sortedWith(compareBy<Order> { it.id != presetOrderId }.thenBy { it.createdAt })
    }

    fun distribute(total: Double?) {
        var remaining = total ?: 0.0
        allocInputs = buildMap {
            candidates.forEach { order ->
                val part = minOf(remaining, dueExcludingThis(order).coerceAtLeast(0.0))
                if (part > MONEY_EPSILON) {
                    put(order.id, formatMoneyInput(part))
                    remaining -= part
                }
            }
        }
    }

    LaunchedEffect(candidates) {
        if (autoAllocate) distribute(parseMoney(amountInput))
    }

    val amount = parseMoney(amountInput)
    val paidAtMs = parseUaDateToEpochMs(dateInput)
    val parsedAllocs = allocInputs.filterValues { it.isNotBlank() }.mapValues { parseMoney(it.value) }
    val allocations = parsedAllocs.mapNotNull { (orderId, value) ->
        value?.takeIf { it > MONEY_EPSILON }?.let { PaymentAllocation(orderId, it) }
    }
    val allocated = allocations.sumOf { it.amount }
    val unallocated = (amount ?: 0.0) - allocated

    val validation = when {
        clientId == null -> "Оберіть клієнта"
        paidAtMs == null -> "Невірна дата. Приклад: 25.06.2026"
        amount == null || amount <= MONEY_EPSILON -> "Вкажіть суму надходження"
        parsedAllocs.values.any { it == null || it < 0.0 } -> "Невірна сума в розподілі"
        unallocated < -MONEY_EPSILON -> "Розподілено більше, ніж надійшло"
        else -> null
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (existing == null) "Нове надходження" else "Надходження") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (presetClientId == null) {
                    LabeledDropdown(
                        label = "Від кого",
                        selectedText = clients.find { it.id == clientId }?.displayName ?: "Оберіть клієнта",
                        options = clients.sortedBy { it.displayName.lowercase() },
                        optionLabel = { it.displayName },
                        onSelect = {
                            if (it.id != clientId) {
                                clientId = it.id
                                autoAllocate = true
                            }
                        }
                    )
                } else {
                    Text(
                        clients.find { it.id == clientId }?.displayName ?: "—",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Дата") },
                        singleLine = true,
                        isError = paidAtMs == null,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            if (autoAllocate) distribute(parseMoney(it))
                        },
                        label = { Text("Сума, ₴") },
                        singleLine = true,
                        isError = amountInput.isNotBlank() && amount == null,
                        modifier = Modifier.weight(1f)
                    )
                }

                LabeledDropdown(
                    label = "Спосіб оплати",
                    selectedText = method.labelUa(),
                    options = PaymentMethod.entries,
                    optionLabel = { it.labelUa() },
                    onSelect = { method = it }
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Призначення платежу") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("№ платіжки") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Примітка") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Розподіл по замовленнях", style = MaterialTheme.typography.titleSmall)
                    TextButton(
                        enabled = candidates.isNotEmpty(),
                        onClick = {
                            autoAllocate = true
                            distribute(amount)
                        }
                    ) { Text("Автоматично", fontSize = 12.sp) }
                }

                if (candidates.isEmpty()) {
                    Text(
                        if (clientId == null) "Спершу оберіть клієнта"
                        else "У клієнта немає неоплачених замовлень — сума буде авансом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                candidates.forEach { order ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                order.itemsSummary(),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "від ${FormatUtils.formatDate(order.createdAt)} · сума " +
                                    "${FormatUtils.formatCurrency(order.totalPrice)} · борг " +
                                    FormatUtils.formatCurrency(dueExcludingThis(order).coerceAtLeast(0.0)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val input = allocInputs[order.id].orEmpty()
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                autoAllocate = false
                                allocInputs = allocInputs + (order.id to it)
                            },
                            singleLine = true,
                            isError = input.isNotBlank() && parseMoney(input) == null,
                            placeholder = { Text("0.00") },
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }

                Text(
                    buildString {
                        append("Розподілено: ${FormatUtils.formatCurrency(allocated)}")
                        if (unallocated > MONEY_EPSILON) {
                            append(" · Аванс (не розподілено): ${FormatUtils.formatCurrency(unallocated)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unallocated > MONEY_EPSILON) DesktopColors.warning
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                (error ?: validation?.takeIf { amountInput.isNotBlank() || clientId != null })?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && validation == null,
                onClick = {
                    val request = PaymentUpsertRequest(
                        clientId = clientId ?: return@Button,
                        paidAtEpochMs = paidAtMs ?: return@Button,
                        amount = amount ?: return@Button,
                        method = method,
                        purpose = purpose,
                        reference = reference,
                        notes = notes,
                        allocations = allocations
                    )
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            if (existing == null) ApiClient.createPayment(request)
                            else ApiClient.updatePayment(existing.id, request)
                            onSaved()
                        } catch (e: Exception) {
                            error = e.message ?: "Не вдалося зберегти надходження"
                        } finally {
                            saving = false
                        }
                    }
                }
            ) { Text("Зберегти") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

/** "Оплати" block for one order: progress, received payments and quick "add payment". */
@Composable
internal fun OrderPaymentsCard(
    order: Order,
    clients: List<Client>,
    orders: List<Order>,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var payments by remember(order.id) { mutableStateOf<List<Payment>>(emptyList()) }
    var reloadTick by remember { mutableStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Payment?>(null) }
    var confirmDelete by remember { mutableStateOf<Payment?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(order.id, order.paidAmount, reloadTick) {
        try {
            payments = ApiClient.getPayments(orderId = order.id)
        } catch (e: Exception) {
            error = e.message ?: "Не вдалося завантажити оплати"
        }
    }

    fun changed() {
        reloadTick++
        onChanged()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Оплати", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PaymentBadge(order.paymentStatus)
                }
                Button(
                    onClick = { creating = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Додати оплату", fontSize = 12.sp)
                }
            }

            PaymentProgress(order)

            if (payments.isEmpty()) {
                Text(
                    "Оплат ще не було",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            payments.forEach { payment ->
                val share = payment.allocations.firstOrNull { it.orderId == order.id }?.amount ?: 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${FormatUtils.formatDate(payment.paidAt)} · ${payment.method.labelUa()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val details = listOfNotNull(
                            payment.reference?.let { "№ $it" },
                            payment.purpose,
                            payment.notes,
                            if (payment.amount - share > MONEY_EPSILON)
                                "з платежу ${FormatUtils.formatCurrency(payment.amount)}" else null
                        )
                        if (details.isNotEmpty()) {
                            Text(
                                details.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        FormatUtils.formatCurrency(share),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DesktopColors.success
                    )
                    IconButton(onClick = { editing = payment }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Редагувати", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { confirmDelete = payment }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            "Видалити",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (creating || editing != null) {
        PaymentEditorDialog(
            existing = editing,
            clients = clients,
            orders = orders,
            presetClientId = order.clientId,
            presetOrderId = order.id,
            onDismiss = {
                creating = false
                editing = null
            },
            onSaved = {
                creating = false
                editing = null
                changed()
            }
        )
    }

    confirmDelete?.let { payment ->
        PaymentDeleteDialog(
            payment = payment,
            onDismiss = { confirmDelete = null },
            onConfirm = {
                confirmDelete = null
                scope.launch {
                    try {
                        ApiClient.deletePayment(payment.id)
                        changed()
                    } catch (e: Exception) {
                        error = e.message ?: "Не вдалося видалити оплату"
                    }
                }
            }
        )
    }
}

/** "Сплачено X з Y" with a progress bar and the remaining debt. */
@Composable
internal fun PaymentProgress(order: Order) {
    val fraction = if (order.totalPrice > 0.0) (order.paidAmount / order.totalPrice).coerceIn(0.0, 1.0) else 0.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Сплачено ${FormatUtils.formatCurrency(order.paidAmount)} з ${FormatUtils.formatCurrency(order.totalPrice)}",
                style = MaterialTheme.typography.bodyMedium
            )
            val balanceText = when (order.paymentStatus) {
                PaymentStatus.OVERPAID -> "Переплата ${FormatUtils.formatCurrency(-order.balanceDue)}"
                PaymentStatus.PAID -> "Оплачено повністю"
                else -> "Залишок ${FormatUtils.formatCurrency(order.balanceDue)}"
            }
            Text(
                balanceText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (order.paymentStatus) {
                    PaymentStatus.PAID -> DesktopColors.success
                    PaymentStatus.OVERPAID -> MaterialTheme.colorScheme.primary
                    else -> DesktopColors.warning
                }
            )
        }
        LinearProgressIndicator(
            progress = { fraction.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = DesktopColors.success,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
internal fun PaymentDeleteDialog(payment: Payment, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видалити надходження") },
        text = {
            Text(
                buildString {
                    append("Видалити надходження ${FormatUtils.formatCurrency(payment.amount)} ")
                    append("від ${FormatUtils.formatDate(payment.paidAt)}?")
                    if (payment.allocations.size > 1) {
                        append(" Воно розподілене на ${payment.allocations.size} замовлення — оплата зникне з усіх.")
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Видалити", color = MaterialTheme.colorScheme.onError) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
