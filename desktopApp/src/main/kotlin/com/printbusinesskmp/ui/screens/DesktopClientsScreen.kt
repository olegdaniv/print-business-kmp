package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.models.DeliveryType
import com.printbusinesskmp.models.MONEY_EPSILON
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.Payment
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.DangerZone
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.InfoRow
import com.printbusinesskmp.ui.components.MetricTile
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.components.ScreenHeader
import com.printbusinesskmp.ui.components.SectionCard
import com.printbusinesskmp.ui.components.SearchField
import com.printbusinesskmp.ui.components.SplitPane
import com.printbusinesskmp.ui.components.StatusFilterChips
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.itemsSummary
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun DeliveryType.displayName(): String = when (this) {
    DeliveryType.NOVA_POSHTA_BRANCH -> "Відділення Нової Пошти"
    DeliveryType.NOVA_POSHTA_ADDRESS -> "Адресна доставка НП"
    DeliveryType.DIRECT_ADDRESS -> "Пряма адреса"
}

@Composable
fun DesktopClientsScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var invoicedOrderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Full-screen spinner/error only until the first successful load; refreshes keep
    // the split pane on screen (see DesktopOrdersScreen for the same pattern).
    var loaded by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ClientType?>(null) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            refreshing = true
            error = null
            try {
                clients = ApiClient.getClients()
                orders = runCatching { ApiClient.getOrders() }.getOrDefault(orders)
                payments = runCatching { ApiClient.getPayments() }.getOrDefault(payments)
                invoicedOrderIds = runCatching { ApiClient.getAllInvoices() }
                    .map { list -> list.mapNotNull { it.orderId }.toSet() }
                    .getOrDefault(invoicedOrderIds)
                if (clients.none { it.id == selectedClientId }) {
                    selectedClientId = clients.firstOrNull()?.id
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

    val filtered = remember(clients, search, typeFilter) {
        val q = search.trim().lowercase()
        // Phones are stored as bare digits, so compare digits-only to match "067 123..."
        val qDigits = q.filter { it.isDigit() }
        clients.filter { client ->
            val matchesSearch = q.isEmpty() ||
                client.displayName.lowercase().contains(q) ||
                (qDigits.isNotEmpty() && client.phone.contains(qDigits)) ||
                client.email?.lowercase()?.contains(q) == true ||
                client.taxId?.contains(q) == true
            val matchesType = typeFilter == null || client.type == typeFilter
            matchesSearch && matchesType
        }
    }

    val selectedClient = selectedClientId?.let { id -> clients.find { it.id == id } }
    val ordersByClient = remember(orders) { orders.groupBy { it.clientId } }
    val debtByClient = remember(orders, invoicedOrderIds) {
        orders.filter { it.isReceivable(hasInvoice = it.id in invoicedOrderIds) }
            .groupBy { it.clientId }
            .mapValues { (_, list) -> list.sumOf { it.balanceDue } }
    }

    SplitPane(
        initialRatio = 0.35f,
        minLeftFraction = 0.25f,
        maxLeftFraction = 0.5f,
        leftContent = {
            ClientListPanel(
                clients = filtered,
                debtByClient = debtByClient,
                search = search,
                onSearchChange = { search = it },
                typeFilter = typeFilter,
                onTypeFilterChange = { typeFilter = it },
                selectedClientId = selectedClientId,
                onSelectClient = { selectedClientId = it },
                onNewClient = { onNavigate(Screen.ClientForm(null)) },
                onRefresh = { load() },
                refreshing = refreshing,
                error = error
            )
        },
        rightContent = {
            if (selectedClient != null) {
                key(selectedClient.id) {
                    ClientDetailPanel(
                        client = selectedClient,
                        orders = ordersByClient[selectedClient.id].orEmpty(),
                        payments = payments.filter { it.clientId == selectedClient.id },
                        debt = debtByClient[selectedClient.id] ?: 0.0,
                        onOpenOrder = { onNavigate(Screen.OrderDetail(it)) },
                        onEdit = { onNavigate(Screen.ClientForm(selectedClient.id)) },
                        onDelete = {
                            scope.launch {
                                try {
                                    ApiClient.deleteClient(selectedClient.id)
                                    selectedClientId = null
                                    load()
                                } catch (e: Exception) {
                                    error = e.message ?: "Не вдалося видалити клієнта"
                                }
                            }
                        }
                    )
                }
            } else {
                EmptyClientPanel()
            }
        }
    )
}

@Composable
private fun ClientListPanel(
    clients: List<Client>,
    debtByClient: Map<String, Double>,
    search: String,
    onSearchChange: (String) -> Unit,
    typeFilter: ClientType?,
    onTypeFilterChange: (ClientType?) -> Unit,
    selectedClientId: String?,
    onSelectClient: (String) -> Unit,
    onNewClient: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Клієнти",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "Оновити", modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = onNewClient,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Додати", fontSize = 13.sp)
                }
            }
        }

        SearchField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = "Пошук за ім'ям, телефоном...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        StatusFilterChips(
            values = ClientType.entries,
            selected = typeFilter,
            onSelect = onTypeFilterChange,
            labelMapper = { if (it == ClientType.PERSON) "Фізособи" else "Компанії" },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = FormatUtils.countUa(clients.size, "клієнт", "клієнти", "клієнтів"),
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

        if (clients.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Немає клієнтів",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(clients, key = { it.id }) { client ->
                    ClientListItem(
                        client = client,
                        debt = debtByClient[client.id] ?: 0.0,
                        selected = client.id == selectedClientId,
                        onClick = { onSelectClient(client.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientListItem(
    client: Client,
    debt: Double,
    selected: Boolean,
    onClick: () -> Unit
) {
    HoverableRow(onClick = onClick, selected = selected) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (client.type == ClientType.PERSON) "Фізособа" else "Компанія",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = FormatUtils.formatPhone(client.phone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            client.delivery?.let { d ->
                Text(
                    text = "${d.type.displayName()}: ${d.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (client.orderCount > 0 || debt > MONEY_EPSILON) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FormatUtils.countUa(client.orderCount, "замовлення", "замовлення", "замовлень"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (debt > MONEY_EPSILON) {
                        Text(
                            text = "борг ${FormatUtils.formatCurrency(debt)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = DesktopColors.warning
                        )
                    }
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
private fun ClientDetailPanel(
    client: Client,
    orders: List<Order>,
    payments: List<Payment>,
    debt: Double,
    onOpenOrder: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
    val paid = activeOrders.sumOf { it.paidAmount }
    val advance = payments.sumOf { it.unallocatedAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = client.displayName,
            subtitle = (if (client.type == ClientType.PERSON) "Фізична особа" else "Юридична особа") +
                " · клієнт з ${FormatUtils.formatDate(client.createdAt)}"
        ) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Редагувати")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = "Замовлень",
                value = activeOrders.size.toString(),
                hint = "на ${FormatUtils.formatCurrency(activeOrders.sumOf { it.totalPrice })}",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Сплачено",
                value = FormatUtils.formatCurrency(paid),
                hint = FormatUtils.countUa(payments.size, "надходження", "надходження", "надходжень"),
                valueColor = DesktopColors.success,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = if (advance > MONEY_EPSILON) "Борг / аванс" else "Борг",
                value = FormatUtils.formatCurrency(debt),
                hint = if (advance > MONEY_EPSILON) "аванс ${FormatUtils.formatCurrency(advance)}"
                else if (debt > MONEY_EPSILON) "очікується оплата" else "боргів немає",
                valueColor = if (debt > MONEY_EPSILON) DesktopColors.warning else DesktopColors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Контакти", modifier = Modifier.weight(1f).fillMaxHeight()) {
                InfoRow("Телефон", FormatUtils.formatPhone(client.phone), labelWidth = 120.dp)
                client.email?.let { InfoRow("Email", it, labelWidth = 120.dp) }
                InfoRow("Адреса", client.address, labelWidth = 120.dp)
                client.contactName?.let { InfoRow("Контактна особа", it, labelWidth = 120.dp) }
            }
            client.delivery?.let { d ->
                SectionCard(title = "Доставка", modifier = Modifier.weight(1f).fillMaxHeight()) {
                    InfoRow("Спосіб", d.type.displayName(), labelWidth = 120.dp)
                    when (d.type) {
                        DeliveryType.NOVA_POSHTA_BRANCH -> {
                            d.city?.let { InfoRow("Місто", it, labelWidth = 120.dp) }
                            d.branch?.let { InfoRow("Відділення", it, labelWidth = 120.dp) }
                        }
                        DeliveryType.NOVA_POSHTA_ADDRESS -> {
                            d.city?.let { InfoRow("Місто", it, labelWidth = 120.dp) }
                            d.street?.let { street ->
                                val full = if (d.building != null) "$street, ${d.building}" else street
                                InfoRow("Вулиця / буд.", full, labelWidth = 120.dp)
                            }
                        }
                        DeliveryType.DIRECT_ADDRESS -> {
                            d.freeAddress?.let { InfoRow("Адреса", it, labelWidth = 120.dp) }
                        }
                    }
                }
            }
        }

        if (client.taxId != null || client.iban != null || client.bankName != null || client.discountPercent != null) {
            SectionCard(title = "Реквізити") {
                client.taxId?.let { InfoRow("ЄДРПОУ / РНОКПП", it) }
                client.iban?.let { InfoRow("IBAN", it) }
                client.bankName?.let { InfoRow("Банк", it) }
                client.discountPercent?.let {
                    InfoRow("Знижка", "${FormatUtils.formatDecimal(it)}%", valueColor = DesktopColors.success, bold = true)
                }
            }
        }

        SectionCard(
            title = "Замовлення",
            subtitle = if (orders.isEmpty()) null else "Натисніть, щоб відкрити"
        ) {
            if (orders.isEmpty()) {
                Text(
                    "Замовлень ще немає",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            orders.sortedByDescending { it.createdAt }.forEachIndexed { index, order ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                HoverableRow(onClick = { onOpenOrder(order.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                order.itemsSummary(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${FormatUtils.formatDate(order.createdAt)} · ${order.status.labelUa()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                FormatUtils.formatCurrency(order.totalPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            PaymentBadge(order.paymentStatus)
                        }
                    }
                }
            }
        }

        client.notes?.takeIf { it.isNotBlank() }?.let { note ->
            SectionCard(title = "Примітки") {
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            "Створено ${FormatUtils.formatDateTime(client.createdAt)} · оновлено ${FormatUtils.formatDateTime(client.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DangerZone(title = "Видалити клієнта", buttonLabel = "Видалити", onClick = { confirmDelete = true })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити клієнта") },
            text = { Text("Підтвердьте видалення клієнта ${client.displayName}. Цю дію неможливо скасувати.") },
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
private fun EmptyClientPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Оберіть клієнта",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
