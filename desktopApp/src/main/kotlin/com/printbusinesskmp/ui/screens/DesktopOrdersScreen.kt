package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.DangerZone
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.ScreenHeader
import com.printbusinesskmp.ui.components.SectionCard
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.components.SearchField
import com.printbusinesskmp.ui.components.SplitPane
import com.printbusinesskmp.ui.components.StatusBadge
import com.printbusinesskmp.ui.components.StatusFilterChips
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.itemsSummary
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopOrdersScreen(onNavigate: (Screen) -> Unit, initialOrderId: String? = null) {
    val scope = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    // `loaded` gates the full-screen spinner to the first load only. Later refreshes
    // (after an invoice is generated, an order deleted, ...) keep the split pane and the
    // detail panel alive, so their scroll position and status messages survive.
    var loaded by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedOrderId by remember { mutableStateOf(initialOrderId) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            refreshing = true
            error = null
            try {
                clients = ApiClient.getClients()
                orders = ApiClient.getOrders()
                // Auto-select the most recent order if nothing (valid) is selected
                if (orders.none { it.id == selectedOrderId }) {
                    selectedOrderId = orders.maxByOrNull { it.updatedAt }?.id
                }
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

    if (!loaded && error == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { load() }) { Text("Повторити") }
            }
        }
        return
    }

    val clientById = remember(clients) { clients.associateBy { it.id } }

    val filtered = remember(orders, clientById, search, statusFilter) {
        val q = search.trim().lowercase()
        orders
            .filter { order ->
                val matchesSearch = q.isEmpty() ||
                    order.id.lowercase().contains(q) ||
                    order.itemsSummary().lowercase().contains(q) ||
                    clientById[order.clientId]?.displayName?.lowercase()?.contains(q) == true ||
                    order.notes?.lowercase()?.contains(q) == true
                val matchesStatus = statusFilter == null || order.status == statusFilter
                matchesSearch && matchesStatus
            }
            .sortedByDescending { it.updatedAt }
    }

    val selectedOrder = selectedOrderId?.let { id -> orders.find { it.id == id } }

    SplitPane(
        initialRatio = 0.35f,
        minLeftFraction = 0.25f,
        maxLeftFraction = 0.5f,
        leftContent = {
            OrderListPanel(
                orders = filtered,
                clientById = clientById,
                search = search,
                onSearchChange = { search = it },
                statusFilter = statusFilter,
                onStatusFilterChange = { statusFilter = it },
                selectedOrderId = selectedOrderId,
                onSelectOrder = { selectedOrderId = it },
                onNewOrder = { onNavigate(Screen.OrderForm(null)) },
                onRefresh = { load() },
                refreshing = refreshing,
                error = error
            )
        },
        rightContent = {
            if (selectedOrder != null) {
                // Keyed by id: dialogs, messages and scroll belong to one order and must
                // not leak into the next one when the selection changes.
                key(selectedOrder.id) {
                    OrderDetailPanel(
                        order = selectedOrder,
                        client = clientById[selectedOrder.clientId],
                        clients = clients,
                        orders = orders,
                        onEdit = { onNavigate(Screen.OrderForm(selectedOrder.id)) },
                        onNavigate = onNavigate,
                        onOrderUpdated = { load() },
                        onDelete = {
                            scope.launch {
                                try {
                                    ApiClient.deleteOrder(selectedOrder.id)
                                    selectedOrderId = null
                                    load()
                                } catch (e: Exception) {
                                    error = e.message ?: "Не вдалося видалити замовлення"
                                }
                            }
                        }
                    )
                }
            } else {
                EmptyDetailPanel()
            }
        }
    )
}

@Composable
private fun OrderListPanel(
    orders: List<Order>,
    clientById: Map<String, Client>,
    search: String,
    onSearchChange: (String) -> Unit,
    statusFilter: OrderStatus?,
    onStatusFilterChange: (OrderStatus?) -> Unit,
    selectedOrderId: String?,
    onSelectOrder: (String) -> Unit,
    onNewOrder: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Замовлення",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "Оновити", modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = onNewOrder,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Нове", fontSize = 13.sp)
                }
            }
        }

        // Search
        SearchField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = "Пошук за назвою, клієнтом...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Status filter chips (wrap to new lines: the list pane can be as narrow as 25%)
        val filterStatuses = listOf(
            OrderStatus.DRAFT,
            OrderStatus.IN_PRODUCTION,
            OrderStatus.READY,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        )

        StatusFilterChips(
            values = filterStatuses,
            selected = statusFilter,
            onSelect = onStatusFilterChange,
            labelMapper = { it.labelUa() },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Count
        Text(
            text = FormatUtils.countUa(orders.size, "замовлення", "замовлення", "замовлень"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(1.dp))
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Order list
        if (orders.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Немає замовлень",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(orders, key = { it.id }) { order ->
                    OrderListItem(
                        order = order,
                        clientName = clientById[order.clientId]?.displayName ?: "—",
                        selected = order.id == selectedOrderId,
                        onClick = { onSelectOrder(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderListItem(
    order: Order,
    clientName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    HoverableRow(
        onClick = onClick,
        selected = selected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.itemsSummary(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = FormatUtils.formatCurrency(order.totalPrice),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = clientName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(order.status)
                    PaymentBadge(order.paymentStatus)
                }
                Text(
                    text = if (order.paymentStatus == com.printbusinesskmp.models.PaymentStatus.PARTIAL)
                        "сплачено ${FormatUtils.formatCurrency(order.paidAmount)}"
                    else FormatUtils.formatDate(order.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun OrderDetailPanel(
    order: Order,
    client: Client?,
    clients: List<Client>,
    orders: List<Order>,
    onEdit: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onOrderUpdated: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var invoices by remember(order.id) { mutableStateOf<List<com.printbusinesskmp.models.Invoice>>(emptyList()) }
    var loadingInvoices by remember(order.id) { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var renamingInvoice by remember { mutableStateOf<com.printbusinesskmp.models.Invoice?>(null) }
    var numberInput by remember { mutableStateOf("") }
    // Date-change dialog: target invoice + whether it edits the ВН (true) or the invoice (false).
    var dateEditInvoice by remember { mutableStateOf<com.printbusinesskmp.models.Invoice?>(null) }
    var dateEditIsVn by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf("") }
    // Delete-document confirmation: target invoice + whether it deletes the ВН or the invoice.
    var confirmDeleteDoc by remember { mutableStateOf<com.printbusinesskmp.models.Invoice?>(null) }
    var confirmDeleteIsVn by remember { mutableStateOf(false) }
    var sentDialogInvoice by remember { mutableStateOf<com.printbusinesskmp.models.Invoice?>(null) }
    // Bumped after a delivery note is (re)generated so its number/date refreshes.
    var deliveryNoteTick by remember { mutableStateOf(0) }

    LaunchedEffect(order.id) {
        loadingInvoices = true
        try {
            invoices = ApiClient.getInvoicesByOrderId(order.id)
        } catch (_: Exception) {
        } finally {
            loadingInvoices = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = order.itemsSummary(),
            subtitle = "${client?.displayName ?: "Невідомий клієнт"} · #${order.id.take(8)}"
        ) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Редагувати")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(order.status)
        }

        order.notes?.takeIf { it.isNotBlank() }?.let { note ->
            SectionCard(title = "Примітки") {
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard(title = "Позиції (${order.items.size})") {
            ItemsTableRow(
                name = "Найменування",
                quantity = "К-сть",
                unitPrice = "Ціна",
                total = "Сума",
                header = true
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            order.items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                val unitPrice = if (item.quantity > 0) item.price / item.quantity else item.price
                ItemsTableRow(
                    name = item.name?.takeIf { it.isNotBlank() }
                        ?: "${item.serviceType.labelUa()} / ${item.productType.labelUa()}",
                    quantity = "${item.quantity} ${item.unit}",
                    unitPrice = FormatUtils.formatCurrency(unitPrice),
                    total = FormatUtils.formatCurrency(item.price)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (order.discountAmount > 0) {
                SummaryRow("Знижка", "-${FormatUtils.formatCurrency(order.discountAmount)}")
            }
            SummaryRow("Разом", FormatUtils.formatCurrency(order.totalPrice), bold = true)
            // Profit only says something when a cost was entered; otherwise it equals the price.
            if (order.totalCost > 0.0) {
                SummaryRow("Собівартість", FormatUtils.formatCurrency(order.totalCost))
                SummaryRow(
                    label = "Прибуток",
                    value = FormatUtils.formatCurrency(order.profit),
                    valueColor = if (order.profit >= 0) DesktopColors.success else MaterialTheme.colorScheme.error,
                    bold = true
                )
            }
        }

        OrderPaymentsCard(
            order = order,
            clients = clients,
            orders = orders,
            onChanged = onOrderUpdated
        )

        // Invoices section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Рахунки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!loadingInvoices && invoices.isEmpty()) {
                        Button(
                            onClick = {
                                processing = true
                                scope.launch {
                                    try {
                                        val inv = ApiClient.generateInvoice(order.id)
                                        val saved = com.printbusinesskmp.desktop.platform.generateInvoiceToFolder(inv)
                                        invoices = ApiClient.getInvoicesByOrderId(order.id)
                                        info = "Рахунок згенеровано: $saved"
                                        onOrderUpdated()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Помилка"
                                    } finally {
                                        processing = false
                                    }
                                }
                            },
                            enabled = !processing,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.success)
                        ) {
                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Згенерувати", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (loadingInvoices) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (invoices.isEmpty()) {
                    Text(
                        "Рахунків поки немає",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        invoices.forEach { invoice ->
                            val vnNumber = remember(invoice.id, deliveryNoteTick) {
                                com.printbusinesskmp.desktop.platform.AppSettingsStore.existingDeliveryNoteNumber(invoice.id)
                            }
                            val issuedDate = FormatUtils.formatDate(invoice.issuedAt)
                            val vnDate = remember(invoice.id, deliveryNoteTick) {
                                FormatUtils.formatDate(
                                    com.printbusinesskmp.desktop.platform.deliveryNoteIssuedDate(invoice)
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Рахунок-фактура
                                    DocumentSection(
                                        label = "Рахунок-фактура",
                                        number = "№ ${invoice.number}",
                                        date = issuedDate,
                                        status = invoice.sentAt
                                            ?.let { "Надіслано ${FormatUtils.formatDate(it)}" }
                                            ?: "Не надіслано клієнту"
                                    ) {
                                        TextButton(onClick = { sentDialogInvoice = invoice }) {
                                            Text(if (invoice.sentAt == null) "Надіслано" else "Дата надсилання", fontSize = 12.sp)
                                        }
                                        TextButton(onClick = {
                                            numberInput = invoice.number
                                            renamingInvoice = invoice
                                        }) { Text("Змінити №", fontSize = 12.sp) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                try {
                                                    val refreshed = ApiClient.regenerateInvoice(invoice.id)
                                                    val saved = com.printbusinesskmp.desktop.platform.generateInvoiceToFolder(refreshed)
                                                    invoices = ApiClient.getInvoicesByOrderId(order.id)
                                                    info = "PDF збережено: $saved"
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Помилка"
                                                }
                                            }
                                        }) { Text("Перегенерувати", fontSize = 12.sp) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                try {
                                                    val opened = com.printbusinesskmp.desktop.platform.openInvoiceFromFolder(invoice)
                                                    if (!opened) error = "Файл не знайдено. Натисніть «Перегенерувати»."
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Помилка"
                                                }
                                            }
                                        }) { Text("Відкрити", fontSize = 12.sp) }
                                        TextButton(onClick = {
                                            dateInput = issuedDate
                                            dateEditIsVn = false
                                            dateEditInvoice = invoice
                                        }) { Text("Змінити дату", fontSize = 12.sp) }
                                        TextButton(onClick = {
                                            confirmDeleteIsVn = false
                                            confirmDeleteDoc = invoice
                                        }) { Text("Видалити", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Видаткова накладна
                                    DocumentSection(
                                        label = "Видаткова накладна",
                                        number = vnNumber?.let { "№ $it" } ?: "Ще не створена",
                                        date = if (vnNumber != null) vnDate else null
                                    ) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                try {
                                                    val saved = com.printbusinesskmp.desktop.platform.generateDeliveryNoteToFolder(invoice)
                                                    deliveryNoteTick++
                                                    info = "Видаткову накладну збережено: $saved"
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Помилка"
                                                }
                                            }
                                        }) { Text(if (vnNumber == null) "Створити" else "Перегенерувати", fontSize = 12.sp) }
                                        TextButton(
                                            enabled = vnNumber != null,
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        val opened = com.printbusinesskmp.desktop.platform.openDeliveryNoteFromFolder(invoice)
                                                        if (!opened) error = "Файл не знайдено. Натисніть «Перегенерувати»."
                                                    } catch (e: Exception) {
                                                        error = e.message ?: "Помилка"
                                                    }
                                                }
                                            }
                                        ) { Text("Відкрити", fontSize = 12.sp) }
                                        TextButton(
                                            enabled = vnNumber != null,
                                            onClick = {
                                                dateInput = vnDate
                                                dateEditIsVn = true
                                                dateEditInvoice = invoice
                                            }
                                        ) { Text("Змінити дату", fontSize = 12.sp) }
                                        TextButton(
                                            enabled = vnNumber != null,
                                            onClick = {
                                                confirmDeleteIsVn = true
                                                confirmDeleteDoc = invoice
                                            }
                                        ) { Text("Видалити", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Messages
        info?.let {
            Text(it, color = DesktopColors.success, style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Text(
            "Створено ${FormatUtils.formatDateTime(order.createdAt)} · оновлено ${FormatUtils.formatDateTime(order.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DangerZone(title = "Видалити замовлення", buttonLabel = "Видалити", onClick = { confirmDelete = true })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити замовлення") },
            text = { Text("Підтвердьте видалення замовлення «${order.itemsSummary()}». Цю дію неможливо скасувати.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    renamingInvoice?.let { invoice ->
        AlertDialog(
            onDismissRequest = { renamingInvoice = null },
            title = { Text("Номер рахунку") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = numberInput,
                    onValueChange = { numberInput = it },
                    label = { Text("Номер") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newNumber = numberInput.trim()
                        if (newNumber.isBlank()) return@Button
                        renamingInvoice = null
                        scope.launch {
                            try {
                                val oldPath = com.printbusinesskmp.desktop.platform.invoiceFilePath(invoice)
                                val renamed = ApiClient.updateInvoiceNumber(invoice.id, newNumber)
                                withContext(Dispatchers.IO) { java.nio.file.Files.deleteIfExists(oldPath) }
                                val saved = com.printbusinesskmp.desktop.platform.generateInvoiceToFolder(renamed)
                                invoices = ApiClient.getInvoicesByOrderId(order.id)
                                info = "Номер змінено. PDF збережено: $saved"
                            } catch (e: Exception) {
                                error = e.message ?: "Помилка"
                            }
                        }
                    }
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingInvoice = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    dateEditInvoice?.let { invoice ->
        val isVn = dateEditIsVn
        val parsedMs = parseUaDateToEpochMs(dateInput)
        AlertDialog(
            onDismissRequest = { dateEditInvoice = null },
            title = { Text(if (isVn) "Дата видаткової накладної" else "Дата рахунку-фактури") },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Дата (дд.мм.рррр)") },
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
                Button(
                    enabled = parsedMs != null,
                    onClick = {
                        val ms = parsedMs ?: return@Button
                        dateEditInvoice = null
                        scope.launch {
                            try {
                                if (isVn) {
                                    // Remove the old-dated PDF first (the filename embeds the date).
                                    val oldPath = com.printbusinesskmp.desktop.platform.deliveryNoteFilePath(invoice)
                                    withContext(Dispatchers.IO) { java.nio.file.Files.deleteIfExists(oldPath) }
                                    com.printbusinesskmp.desktop.platform.AppSettingsStore.setDeliveryNoteDateMillis(invoice.id, ms)
                                    val saved = com.printbusinesskmp.desktop.platform.generateDeliveryNoteToFolder(invoice)
                                    deliveryNoteTick++
                                    info = "Дату ВН змінено. PDF збережено: $saved"
                                } else {
                                    val oldPath = com.printbusinesskmp.desktop.platform.invoiceFilePath(invoice)
                                    val updated = ApiClient.updateInvoiceDate(invoice.id, ms)
                                    withContext(Dispatchers.IO) { java.nio.file.Files.deleteIfExists(oldPath) }
                                    val saved = com.printbusinesskmp.desktop.platform.generateInvoiceToFolder(updated)
                                    invoices = ApiClient.getInvoicesByOrderId(order.id)
                                    deliveryNoteTick++
                                    info = "Дату рахунку змінено. PDF збережено: $saved"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Помилка"
                            }
                        }
                    }
                ) { Text("Зберегти") }
            },
            dismissButton = {
                TextButton(onClick = { dateEditInvoice = null }) { Text("Скасувати") }
            }
        )
    }

    sentDialogInvoice?.let { invoice ->
        InvoiceSentDialog(
            invoice = invoice,
            onDismiss = { sentDialogInvoice = null },
            onSaved = { updated ->
                invoices = invoices.map { if (it.id == updated.id) updated else it }
                info = updated.sentAt?.let { "Рахунок позначено надісланим ${FormatUtils.formatDate(it)}" }
                    ?: "Позначку про надсилання знято"
            },
            onError = { error = it }
        )
    }

    confirmDeleteDoc?.let { invoice ->
        val isVn = confirmDeleteIsVn
        AlertDialog(
            onDismissRequest = { confirmDeleteDoc = null },
            title = { Text(if (isVn) "Видалити видаткову накладну" else "Видалити рахунок-фактуру") },
            text = {
                Text(
                    if (isVn) "Видалити видаткову накладну для рахунку № ${invoice.number}? PDF буде видалено."
                    else "Видалити рахунок № ${invoice.number} разом із видатковою накладною та файлами PDF? Цю дію неможливо скасувати."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteDoc = null
                        scope.launch {
                            try {
                                if (isVn) {
                                    com.printbusinesskmp.desktop.platform.deleteDeliveryNote(invoice)
                                    deliveryNoteTick++
                                    info = "Видаткову накладну видалено"
                                } else {
                                    com.printbusinesskmp.desktop.platform.deleteInvoiceDocuments(invoice)
                                    ApiClient.deleteInvoice(invoice.id)
                                    invoices = ApiClient.getInvoicesByOrderId(order.id)
                                    deliveryNoteTick++
                                    info = "Рахунок видалено"
                                    onOrderUpdated()
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Помилка"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Видалити", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDoc = null }) { Text("Скасувати") }
            }
        )
    }
}

/**
 * One document line inside an invoice card: a label + number/date on the left
 * and its actions (FlowRow) on the right.
 */
@Composable
private fun DocumentSection(
    label: String,
    number: String,
    date: String?,
    status: String? = null,
    actions: @Composable () -> Unit
) {
    // Info on top, actions below: a side-by-side row squeezed the number into a sliver
    // once the invoice got its "Надіслано" action.
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            buildString {
                append(number)
                if (date != null) append(" · $date")
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            actions()
        }
    }
}

/** One line of the order items table; [header] renders the column captions. */
@Composable
private fun ItemsTableRow(
    name: String,
    quantity: String,
    unitPrice: String,
    total: String,
    header: Boolean = false
) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (header) 0.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = style,
            color = color,
            fontWeight = if (header) null else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(quantity, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
        Text(unitPrice, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(120.dp))
        Text(
            total,
            style = style,
            color = color,
            fontWeight = if (header) null else FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(130.dp)
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyDetailPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Оберіть замовлення",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
