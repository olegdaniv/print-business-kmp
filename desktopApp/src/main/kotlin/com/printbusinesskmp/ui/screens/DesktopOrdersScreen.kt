package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
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
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.components.SearchField
import com.printbusinesskmp.ui.components.SplitPane
import com.printbusinesskmp.ui.components.StatusBadge
import com.printbusinesskmp.ui.components.StatusFilterChips
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch

@Composable
fun DesktopOrdersScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                clients = ApiClient.getClients()
                orders = ApiClient.getOrders()
                // Auto-select first order if none selected
                if (selectedOrderId == null && orders.isNotEmpty()) {
                    selectedOrderId = orders.sortedByDescending { it.updatedAt }.first().id
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { load() }) { Text("Повторити") }
            }
        }
        return
    }

    val clientById = clients.associateBy { it.id }

    val filtered = orders
        .filter { order ->
            val matchesSearch = search.isBlank() || run {
                val q = search.lowercase()
                order.id.lowercase().contains(q) ||
                    clientById[order.clientId]?.displayName?.lowercase()?.contains(q) == true ||
                    order.notes?.lowercase()?.contains(q) == true
            }
            val matchesStatus = statusFilter == null || order.status == statusFilter
            matchesSearch && matchesStatus
        }
        .sortedByDescending { it.updatedAt }

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
                onRefresh = { load() }
            )
        },
        rightContent = {
            if (selectedOrder != null) {
                OrderDetailPanel(
                    order = selectedOrder,
                    client = clientById[selectedOrder.clientId],
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
                                error = e.message
                            }
                        }
                    }
                )
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
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                    contentPadding = ButtonDefaults.ContentPadding.let {
                        androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    },
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
            placeholder = "Пошук за ID, клієнтом...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Status filter chips
        @Suppress("DEPRECATION")
        val filterStatuses = listOf(
            OrderStatus.DRAFT,
            OrderStatus.IN_PRODUCTION,
            OrderStatus.READY,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
//            StatusFilterChips(
//                values = filterStatuses,
//                selected = statusFilter,
//                onSelect = onStatusFilterChange,
//                labelMapper = { it.labelUa() }
//            )
        }

        Spacer(Modifier.height(8.dp))

        // Count
        Text(
            text = "${orders.size} замовлень",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                    text = "#${order.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
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
                    text = FormatUtils.formatDate(order.updatedAt),
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Замовлення #${order.id.take(8)}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = client?.displayName ?: "Невідомий клієнт",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Редагувати")
                }
            }
        }

        // Status row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(order.status)
            PaymentBadge(order.paymentStatus)
        }

        // Dates
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Створено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatDateTime(order.createdAt), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Оновлено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatDateTime(order.updatedAt), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Notes
        order.notes?.takeIf { it.isNotBlank() }?.let { note ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Примітки", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Order items
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Позиції (${order.items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                order.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.serviceType.labelUa()} / ${item.productType.labelUa()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "К-сть: ${item.quantity} · ${FormatUtils.formatDecimal(item.usedMeters)} м",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            item.size?.let { size ->
                                Text(
                                    text = "Розмір: $size",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item.color?.let { color ->
                                Text(
                                    text = "Колір: $color",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatCurrency(item.price),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Собівартість: ${FormatUtils.formatCurrency(item.cost)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatCurrency(item.profit),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.profit >= 0) DesktopColors.success else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Cost summary
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryRow("Собівартість", FormatUtils.formatCurrency(order.totalCost))
                if (order.discountAmount > 0) {
                    SummaryRow("Знижка", "-${FormatUtils.formatCurrency(order.discountAmount)}")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryRow(
                    "Ціна",
                    FormatUtils.formatCurrency(order.totalPrice),
                    bold = true
                )
                SummaryRow(
                    "Прибуток",
                    FormatUtils.formatCurrency(order.profit),
                    valueColor = if (order.profit >= 0) DesktopColors.success else MaterialTheme.colorScheme.error,
                    bold = true
                )
            }
        }

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
                            contentPadding = ButtonDefaults.ContentPadding.let {
                                androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            },
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
                    invoices.forEach { invoice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${invoice.number} · ${FormatUtils.formatDate(invoice.issuedAt)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            val saved = com.printbusinesskmp.desktop.platform.generateInvoiceToFolder(invoice)
                                            info = "PDF збережено: $saved"
                                        } catch (e: Exception) {
                                            error = e.message ?: "Помилка"
                                        }
                                    }
                                }) {
                                    Text("Перегенерувати", fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            val opened = com.printbusinesskmp.desktop.platform.openInvoiceFromFolder(invoice)
                                            if (!opened) {
                                                error = "Файл не знайдено. Натисніть «Перегенерувати»."
                                            }
                                        } catch (e: Exception) {
                                            error = e.message ?: "Помилка"
                                        }
                                    }
                                }) {
                                    Text("Відкрити", fontSize = 12.sp)
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

        // Delete
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Видалити замовлення", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    Text("Ця дія незворотна", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Видалити", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити замовлення") },
            text = { Text("Підтвердьте видалення замовлення #${order.id.take(8)}. Цю дію неможливо скасувати.") },
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
