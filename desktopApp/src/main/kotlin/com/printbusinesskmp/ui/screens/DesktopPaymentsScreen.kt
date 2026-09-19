package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.InfoRow
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.components.SearchField
import com.printbusinesskmp.ui.components.SectionCard
import com.printbusinesskmp.ui.components.SplitPane
import com.printbusinesskmp.ui.components.StatusFilterChips
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.itemsSummary
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

private enum class PaymentsTab(val label: String) {
    REGISTRY("Рахунки"),
    JOURNAL("Надходження")
}

private enum class RegistryFilter(val label: String) {
    AWAITING("Очікують оплати"),
    NOT_SENT("Не надіслані"),
    PAID("Оплачені")
}

/** One billable order with its invoices and the payments that settled it. */
private class RegistryRow(
    val order: Order,
    val client: Client?,
    /** Oldest first. */
    val invoices: List<Invoice>,
    /** Payment + this order's share of it, oldest first. */
    val payments: List<Pair<Payment, Double>>
) {
    val issuedAt: Instant? = invoices.firstOrNull()?.issuedAt
    val sentAt: Instant? = invoices.mapNotNull { it.sentAt }.minOrNull()
    val referenceDate: Instant = issuedAt ?: order.createdAt
    val fullyPaid: Boolean =
        order.paymentStatus == PaymentStatus.PAID || order.paymentStatus == PaymentStatus.OVERPAID
    val receivable: Boolean = order.isReceivable(hasInvoice = invoices.isNotEmpty())
    val invoiceLabel: String =
        invoices.joinToString(", ") { "№ ${it.number}" }.ifEmpty { "Без рахунку" }

    /** Days from sending the invoice to full payment, or waiting so far. */
    val daysLabel: String? = sentAt?.let { sent ->
        val end = if (fullyPaid) order.lastPaidAt ?: Clock.System.now() else Clock.System.now()
        val days = (end - sent).inWholeDays.coerceAtLeast(0)
        if (fullyPaid) "оплачено за $days дн." else "чекає $days дн."
    }

    fun matches(query: String): Boolean =
        query.isEmpty() ||
            client?.displayName?.lowercase()?.contains(query) == true ||
            order.itemsSummary().lowercase().contains(query) ||
            invoices.any { it.number.lowercase().contains(query) }
}

@Composable
fun DesktopPaymentsScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var legacyPartialIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    var tab by remember { mutableStateOf(PaymentsTab.REGISTRY) }
    var period by remember { mutableStateOf<PaymentPeriod?>(null) }
    var search by remember { mutableStateOf("") }
    var registryFilter by remember { mutableStateOf<RegistryFilter?>(RegistryFilter.AWAITING) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var selectedPaymentId by remember { mutableStateOf<String?>(null) }
    var creatingPayment by remember { mutableStateOf(false) }

    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            refreshing = true
            error = null
            try {
                clients = ApiClient.getClients()
                orders = ApiClient.getOrders()
                payments = ApiClient.getPayments()
                invoices = ApiClient.getAllInvoices()
                legacyPartialIds = runCatching { ApiClient.getLegacyPartialOrderIds() }.getOrDefault(emptyList())
                loaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (!loaded) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (error == null) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { load() }) { Text("Повторити") }
                }
            }
        }
        return
    }

    val clientById = remember(clients) { clients.associateBy { it.id } }
    val orderById = remember(orders) { orders.associateBy { it.id } }
    val query = search.trim().lowercase()

    val allRows = remember(orders, invoices, payments, clientById) {
        val invoicesByOrder = invoices.filter { it.orderId != null }.groupBy { it.orderId!! }
        val sharesByOrder = payments
            .flatMap { payment -> payment.allocations.map { it.orderId to (payment to it.amount) } }
            .groupBy({ it.first }, { it.second })
        orders
            .map { order ->
                RegistryRow(
                    order = order,
                    client = clientById[order.clientId],
                    invoices = invoicesByOrder[order.id].orEmpty().sortedBy { it.issuedAt },
                    payments = sharesByOrder[order.id].orEmpty().sortedBy { it.first.paidAt }
                )
            }
            .filter { row ->
                row.invoices.isNotEmpty() || row.order.paidAmount > MONEY_EPSILON ||
                    (row.order.status != OrderStatus.DRAFT && row.order.status != OrderStatus.CANCELLED)
            }
    }

    val rows = allRows
        .filter { period == null || period!!.contains(it.referenceDate) }
        .filter { it.matches(query) }
        .filter { row ->
            when (registryFilter) {
                null -> true
                RegistryFilter.AWAITING -> row.receivable
                RegistryFilter.NOT_SENT -> row.invoices.isNotEmpty() && row.sentAt == null && !row.fullyPaid
                RegistryFilter.PAID -> row.fullyPaid
            }
        }
        .sortedByDescending { it.referenceDate }

    val journal = payments
        .filter { period == null || period!!.contains(it.paidAt) }
        .filter { payment ->
            query.isEmpty() ||
                clientById[payment.clientId]?.displayName?.lowercase()?.contains(query) == true ||
                payment.purpose?.lowercase()?.contains(query) == true ||
                payment.reference?.lowercase()?.contains(query) == true
        }
        .sortedByDescending { it.paidAt }

    val legacyRows = allRows.filter { it.order.id in legacyPartialIds }

    // Keep the detail pane filled, like the orders screen does.
    LaunchedEffect(tab, rows.map { it.order.id }, journal.map { it.id }) {
        if (tab == PaymentsTab.REGISTRY && rows.none { it.order.id == selectedOrderId }) {
            selectedOrderId = rows.firstOrNull()?.order?.id ?: selectedOrderId
        }
        if (tab == PaymentsTab.JOURNAL && journal.none { it.id == selectedPaymentId }) {
            selectedPaymentId = journal.firstOrNull()?.id
        }
    }

    SplitPane(
        initialRatio = 0.45f,
        minLeftFraction = 0.3f,
        maxLeftFraction = 0.6f,
        leftContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Оплати",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { load() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, "Оновити", modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { creatingPayment = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Надходження", fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentsTab.entries.forEach { entry ->
                        FilterChip(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            label = { Text(entry.label, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                SearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = if (tab == PaymentsTab.REGISTRY) "Клієнт, № рахунку, позиції..."
                    else "Клієнт, призначення, № платіжки...",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(8.dp))

                StatusFilterChips(
                    allLabel = "Весь час",
                    values = PaymentPeriod.entries,
                    selected = period,
                    onSelect = { period = it },
                    labelMapper = { it.label },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (tab == PaymentsTab.REGISTRY) {
                    Spacer(Modifier.height(4.dp))
                    StatusFilterChips(
                        values = RegistryFilter.entries,
                        selected = registryFilter,
                        onSelect = { registryFilter = it },
                        labelMapper = { it.label },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                val summary = if (tab == PaymentsTab.REGISTRY) {
                    val due = rows.filter { it.receivable }.sumOf { it.order.balanceDue }
                    "${FormatUtils.countUa(rows.size, "замовлення", "замовлення", "замовлень")} · " +
                        "до оплати ${FormatUtils.formatCurrency(due)}"
                } else {
                    val unallocated = journal.sumOf { it.unallocatedAmount }
                    buildString {
                        append("Надійшло ${FormatUtils.formatCurrency(journal.sumOf { it.amount })}")
                        append(" · ${FormatUtils.countUa(journal.size, "надходження", "надходження", "надходжень")}")
                        if (unallocated > MONEY_EPSILON) {
                            append(" · аванси ${FormatUtils.formatCurrency(unallocated)}")
                        }
                    }
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (legacyRows.isNotEmpty()) {
                    LegacyPartialBanner(
                        rows = legacyRows,
                        onOpen = { orderId ->
                            tab = PaymentsTab.REGISTRY
                            registryFilter = null
                            selectedOrderId = orderId
                        }
                    )
                }

                if (refreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(1.dp))
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                when (tab) {
                    PaymentsTab.REGISTRY -> if (rows.isEmpty()) {
                        EmptyListHint("Нічого не знайдено")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(rows, key = { it.order.id }) { row ->
                                RegistryListItem(
                                    row = row,
                                    selected = row.order.id == selectedOrderId,
                                    onClick = { selectedOrderId = row.order.id }
                                )
                            }
                        }
                    }

                    PaymentsTab.JOURNAL -> if (journal.isEmpty()) {
                        EmptyListHint("Надходжень немає")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(journal, key = { it.id }) { payment ->
                                JournalListItem(
                                    payment = payment,
                                    clientName = clientById[payment.clientId]?.displayName ?: "—",
                                    orderById = orderById,
                                    selected = payment.id == selectedPaymentId,
                                    onClick = { selectedPaymentId = payment.id }
                                )
                            }
                        }
                    }
                }
            }
        },
        rightContent = {
            when (tab) {
                PaymentsTab.REGISTRY -> {
                    val row = allRows.find { it.order.id == selectedOrderId }
                    if (row == null) {
                        EmptyPaymentsDetail("Оберіть рахунок")
                    } else {
                        key(row.order.id) {
                            RegistryDetailPanel(
                                row = row,
                                clients = clients,
                                orders = orders,
                                onNavigate = onNavigate,
                                onChanged = { load() }
                            )
                        }
                    }
                }

                PaymentsTab.JOURNAL -> {
                    val payment = payments.find { it.id == selectedPaymentId }
                    if (payment == null) {
                        EmptyPaymentsDetail("Оберіть надходження")
                    } else {
                        key(payment.id) {
                            PaymentDetailPanel(
                                payment = payment,
                                client = clientById[payment.clientId],
                                clients = clients,
                                orders = orders,
                                onOpenOrder = { orderId ->
                                    tab = PaymentsTab.REGISTRY
                                    registryFilter = null
                                    selectedOrderId = orderId
                                },
                                onChanged = { load() },
                                onDeleted = {
                                    selectedPaymentId = null
                                    load()
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    if (creatingPayment) {
        PaymentEditorDialog(
            existing = null,
            clients = clients,
            orders = orders,
            onDismiss = { creatingPayment = false },
            onSaved = {
                creatingPayment = false
                load()
            }
        )
    }
}

@Composable
private fun LegacyPartialBanner(rows: List<RegistryRow>, onOpen: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DesktopColors.warningBg),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Позначені «Частково» до обліку оплат — внесіть фактичні суми:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF854D0E)
            )
            rows.forEach { row ->
                Text(
                    "• ${row.client?.displayName ?: "—"} — ${row.order.itemsSummary()} " +
                        "(${FormatUtils.formatCurrency(row.order.totalPrice)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF854D0E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(row.order.id) }
                )
            }
        }
    }
}

@Composable
private fun RegistryListItem(row: RegistryRow, selected: Boolean, onClick: () -> Unit) {
    HoverableRow(onClick = onClick, selected = selected) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${row.invoiceLabel} · ${row.client?.displayName ?: "—"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    FormatUtils.formatCurrency(row.order.totalPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                row.order.itemsSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    row.issuedAt?.let { "Виставлено ${FormatUtils.formatDate(it)}" }
                        ?: "Створено ${FormatUtils.formatDate(row.order.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (row.invoices.isNotEmpty()) {
                    Text("·", style = MaterialTheme.typography.bodySmall)
                    Text(
                        row.sentAt?.let { "Надіслано ${FormatUtils.formatDate(it)}" } ?: "Не надіслано",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.sentAt == null && !row.fullyPaid) DesktopColors.warning
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                if (row.payments.isEmpty()) "Оплат немає"
                else "Оплати: " + row.payments.joinToString("; ") { (payment, share) ->
                    "${FormatUtils.formatDate(payment.paidAt)} — ${FormatUtils.formatCurrency(share)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (row.payments.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else DesktopColors.success,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PaymentBadge(row.order.paymentStatus)
                    if (row.order.balanceDue > MONEY_EPSILON && row.order.paidAmount > MONEY_EPSILON) {
                        Text(
                            "залишок ${FormatUtils.formatCurrency(row.order.balanceDue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesktopColors.warning
                        )
                    }
                }
                row.daysLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun JournalListItem(
    payment: Payment,
    clientName: String,
    orderById: Map<String, Order>,
    selected: Boolean,
    onClick: () -> Unit
) {
    HoverableRow(onClick = onClick, selected = selected) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${FormatUtils.formatDate(payment.paidAt)} · $clientName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    "+${FormatUtils.formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DesktopColors.success
                )
            }
            Text(
                listOfNotNull(payment.method.labelUa(), payment.reference?.let { "№ $it" }, payment.purpose)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            payment.allocations.forEach { allocation ->
                Text(
                    "→ ${orderById[allocation.orderId]?.itemsSummary() ?: "Замовлення"}: " +
                        FormatUtils.formatCurrency(allocation.amount),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (payment.unallocatedAmount > MONEY_EPSILON) {
                Text(
                    "Не розподілено (аванс): ${FormatUtils.formatCurrency(payment.unallocatedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = DesktopColors.warning
                )
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private data class TimelineEvent(val at: Instant, val text: String, val color: Color? = null)

@Composable
private fun RegistryDetailPanel(
    row: RegistryRow,
    clients: List<Client>,
    orders: List<Order>,
    onNavigate: (Screen) -> Unit,
    onChanged: () -> Unit
) {
    var sentDialogInvoice by remember { mutableStateOf<Invoice?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(row.invoiceLabel, style = MaterialTheme.typography.headlineMedium)
                Text(
                    row.client?.displayName ?: "Невідомий клієнт",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    row.order.itemsSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { onNavigate(Screen.OrderDetail(row.order.id)) },
                shape = RoundedCornerShape(8.dp)
            ) { Text("Відкрити замовлення") }
        }

        // Timeline
        val events = buildList {
            add(TimelineEvent(row.order.createdAt, "Замовлення створено"))
            row.invoices.forEach { invoice ->
                add(TimelineEvent(invoice.issuedAt, "Рахунок № ${invoice.number} виставлено"))
                invoice.sentAt?.let { add(TimelineEvent(it, "Рахунок № ${invoice.number} надіслано клієнту")) }
            }
            row.payments.forEach { (payment, share) ->
                add(
                    TimelineEvent(
                        payment.paidAt,
                        "Оплата ${FormatUtils.formatCurrency(share)} · ${payment.method.labelUa()}",
                        DesktopColors.success
                    )
                )
            }
        }.sortedBy { it.at }

        SectionCard(title = "Хронологія") {
            events.forEach { event ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(event.color ?: MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        FormatUtils.formatDate(event.at),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(96.dp)
                    )
                    Text(event.text, style = MaterialTheme.typography.bodyMedium, color = event.color ?: Color.Unspecified)
                }
            }
            if (!row.fullyPaid && row.order.status != OrderStatus.CANCELLED) {
                Text(
                    buildString {
                        append("Очікується ${FormatUtils.formatCurrency(row.order.balanceDue.coerceAtLeast(0.0))}")
                        row.daysLabel?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DesktopColors.warning,
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp)
                )
            } else {
                row.daysLabel?.let {
                    Text(
                        it.replaceFirstChar { c -> c.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 18.dp, top = 4.dp)
                    )
                }
            }
        }

        SectionCard(title = "Рахунки") {
            if (row.invoices.isEmpty()) {
                Text(
                    "Рахунок ще не виставлено — його можна створити в картці замовлення.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            row.invoices.forEach { invoice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "№ ${invoice.number} · ${FormatUtils.formatCurrency(invoice.totalAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Виставлено ${FormatUtils.formatDate(invoice.issuedAt)} · " +
                                (invoice.sentAt?.let { "надіслано ${FormatUtils.formatDate(it)}" } ?: "не надіслано"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (invoice.sentAt == null) DesktopColors.warning
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (invoice.sentAt == null) {
                        Button(
                            onClick = { sentDialogInvoice = invoice },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.success)
                        ) { Text("Надіслано", fontSize = 12.sp) }
                    } else {
                        TextButton(onClick = { sentDialogInvoice = invoice }) {
                            Text("Змінити дату", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        OrderPaymentsCard(
            order = row.order,
            clients = clients,
            orders = orders,
            onChanged = onChanged
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    sentDialogInvoice?.let { invoice ->
        InvoiceSentDialog(
            invoice = invoice,
            onDismiss = { sentDialogInvoice = null },
            onSaved = { onChanged() },
            onError = { error = it }
        )
    }
}

@Composable
private fun PaymentDetailPanel(
    payment: Payment,
    client: Client?,
    clients: List<Client>,
    orders: List<Order>,
    onOpenOrder: (String) -> Unit,
    onChanged: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val orderById = remember(orders) { orders.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    "+${FormatUtils.formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesktopColors.success
                )
                Text(
                    client?.displayName ?: "Невідомий клієнт",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editing = true }, shape = RoundedCornerShape(8.dp)) {
                    Text("Редагувати")
                }
                OutlinedButton(onClick = { confirmDelete = true }, shape = RoundedCornerShape(8.dp)) {
                    Text("Видалити", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        SectionCard(title = "Надходження") {
            InfoRow("Дата", FormatUtils.formatDate(payment.paidAt))
            InfoRow("Спосіб", payment.method.labelUa())
            payment.reference?.let { InfoRow("№ платіжки", it) }
            payment.purpose?.let { InfoRow("Призначення", it) }
            payment.notes?.let { InfoRow("Примітка", it) }
            InfoRow("Внесено", FormatUtils.formatDateTime(payment.createdAt))
        }

        SectionCard(title = "Розподіл") {
            if (payment.allocations.isEmpty()) {
                Text(
                    "Не прив'язано до жодного замовлення",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            payment.allocations.forEach { allocation ->
                val order = orderById[allocation.orderId]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            order?.itemsSummary() ?: "Замовлення видалено",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        order?.let {
                            Text(
                                "Сплачено ${FormatUtils.formatCurrency(it.paidAmount)} з " +
                                    FormatUtils.formatCurrency(it.totalPrice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        FormatUtils.formatCurrency(allocation.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (order != null) {
                        TextButton(onClick = { onOpenOrder(order.id) }) { Text("Рахунок", fontSize = 12.sp) }
                    }
                }
            }
            if (payment.unallocatedAmount > MONEY_EPSILON) {
                Text(
                    "Не розподілено (аванс): ${FormatUtils.formatCurrency(payment.unallocatedAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = DesktopColors.warning
                )
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (editing) {
        PaymentEditorDialog(
            existing = payment,
            clients = clients,
            orders = orders,
            onDismiss = { editing = false },
            onSaved = {
                editing = false
                onChanged()
            }
        )
    }

    if (confirmDelete) {
        PaymentDeleteDialog(
            payment = payment,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        ApiClient.deletePayment(payment.id)
                        onDeleted()
                    } catch (e: Exception) {
                        error = e.message ?: "Не вдалося видалити надходження"
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyListHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyPaymentsDetail(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
