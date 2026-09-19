package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import com.printbusinesskmp.ui.components.MetricTile
import com.printbusinesskmp.ui.components.PaymentBadge
import com.printbusinesskmp.ui.components.ScreenHeader
import com.printbusinesskmp.ui.components.SectionCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.update.UpdateUiState
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.StatCard
import com.printbusinesskmp.ui.components.StatusBadge
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.itemsSummary
import kotlin.time.Duration.Companion.days

@Composable
fun DesktopDashboardScreen(
    onNavigate: (Screen) -> Unit,
    updateState: UpdateUiState = UpdateUiState(),
    onDownloadUpdate: () -> Unit = {},
    onCancelUpdateDownload: () -> Unit = {},
    onInstallUpdate: () -> Unit = {}
) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var payments by remember { mutableStateOf<List<com.printbusinesskmp.models.Payment>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<com.printbusinesskmp.models.Invoice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            clients = ApiClient.getClients()
            orders = ApiClient.getOrders()
            payments = runCatching { ApiClient.getPayments() }.getOrDefault(emptyList())
            invoices = runCatching { ApiClient.getAllInvoices() }.getOrDefault(emptyList())
        } catch (e: Exception) {
            error = e.message ?: "Помилка завантаження"
        } finally {
            loading = false
        }
    }

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
            }
        }
        return
    }

    val now = kotlin.time.Clock.System.now()
    val activeOrders = orders.count { !it.status.isFinal }
    val invoicedOrderIds = invoices.mapNotNull { it.orderId }.toSet()
    val receivables = orders.filter { it.isReceivable(hasInvoice = it.id in invoicedOrderIds) }
    val receivableIds = receivables.map { it.id }.toSet()
    val receivableTotal = receivables.sumOf { it.balanceDue }
    val monthPayments = payments.filter { PaymentPeriod.THIS_MONTH.contains(it.paidAt) }
    val monthOrders = orders.filter { it.status != OrderStatus.CANCELLED && PaymentPeriod.THIS_MONTH.contains(it.createdAt) }
    val notSentInvoices = invoices.count { it.orderId in receivableIds && it.sentAt == null }
    // Invoices sent over a week ago whose order is still not fully paid.
    val overdueSent = invoices.count { inv ->
        inv.orderId in receivableIds && inv.sentAt?.let { it < now - 7.days } == true
    }
    val clientById = clients.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(title = "Огляд", subtitle = "Сьогодні ${FormatUtils.formatDate(now)}") {
            OutlinedButton(onClick = { onNavigate(Screen.Payments) }, shape = RoundedCornerShape(8.dp)) {
                Text("Оплати")
            }
            Button(
                onClick = { onNavigate(Screen.OrderForm(null)) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Text(" Нове замовлення")
            }
        }

        // Update banner — handles the whole update flow inline (download → install)
        val isDownloaded = updateState.downloadedInstaller != null
        if (updateState.updateAvailable || updateState.isDownloading || isDownloaded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = updateState.latestVersion
                                        ?.let { "Доступна нова версія: $it" }
                                        ?: "Доступна нова версія",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = when {
                                        isDownloaded -> "Завантажено та готово до встановлення"
                                        updateState.isDownloading -> buildUpdateProgressText(updateState)
                                        else -> "Бажаєте оновити застосунок?"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        when {
                            isDownloaded -> Button(
                                onClick = { showInstallDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                                Text(" Встановити")
                            }

                            updateState.isDownloading -> TextButton(onClick = onCancelUpdateDownload) {
                                Text("Скасувати")
                            }

                            else -> Button(
                                onClick = onDownloadUpdate,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                                Text(" Оновити")
                            }
                        }
                    }

                    if (updateState.isDownloading) {
                        val progress = updateState.progressFraction
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    if (updateState.errorMessage != null) {
                        Text(
                            text = updateState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile(
                label = "Активні замовлення",
                value = activeOrders.toString(),
                hint = "з ${orders.size} усього",
                onClick = { onNavigate(Screen.Orders) },
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Продажі цього місяця",
                value = FormatUtils.formatCurrency(monthOrders.sumOf { it.totalPrice }),
                hint = FormatUtils.countUa(monthOrders.size, "замовлення", "замовлення", "замовлень"),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Дебіторка",
                value = FormatUtils.formatCurrency(receivableTotal),
                hint = FormatUtils.countUa(receivables.size, "замовлення", "замовлення", "замовлень") + " чекають оплати",
                valueColor = if (receivables.isNotEmpty()) DesktopColors.warning else DesktopColors.success,
                onClick = { onNavigate(Screen.Payments) },
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Надійшло цього місяця",
                value = FormatUtils.formatCurrency(monthPayments.sumOf { it.amount }),
                hint = FormatUtils.countUa(monthPayments.size, "надходження", "надходження", "надходжень"),
                valueColor = DesktopColors.success,
                onClick = { onNavigate(Screen.Payments) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = "Останні замовлення",
                modifier = Modifier.weight(2f),
                trailing = {
                    TextButton(onClick = { onNavigate(Screen.Orders) }) { Text("Дивитись всі") }
                }
            ) {
                val recent = orders.sortedByDescending { it.updatedAt }.take(8)
                if (recent.isEmpty()) {
                    Text(
                        "Замовлень ще немає",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                recent.forEachIndexed { index, order ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    HoverableRow(onClick = { onNavigate(Screen.OrderDetail(order.id)) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    order.itemsSummary(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${clientById[order.clientId]?.displayName ?: "—"} · ${FormatUtils.formatDate(order.updatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    FormatUtils.formatCurrency(order.totalPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(order.status)
                                    PaymentBadge(order.paymentStatus)
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val attention = buildList {
                    if (overdueSent > 0) add(
                        Triple("$overdueSent рах. надіслано понад 7 днів тому й не оплачено", MaterialTheme.colorScheme.error, Screen.Payments as Screen)
                    )
                    if (notSentInvoices > 0) add(
                        Triple("$notSentInvoices рах. ще не надіслано клієнтам", DesktopColors.warning, Screen.Payments as Screen)
                    )
                    val ready = orders.count { it.status == OrderStatus.READY }
                    if (ready > 0) add(
                        Triple(FormatUtils.countUa(ready, "замовлення", "замовлення", "замовлень") + " готові до видачі", MaterialTheme.colorScheme.primary, Screen.Orders as Screen)
                    )
                    val drafts = orders.count { it.status == OrderStatus.DRAFT }
                    if (drafts > 0) add(
                        Triple("Чернетки замовлень: $drafts", MaterialTheme.colorScheme.onSurfaceVariant, Screen.Orders as Screen)
                    )
                }
                SectionCard(title = "Потребує уваги") {
                    if (attention.isEmpty()) {
                        Text(
                            "Все під контролем",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DesktopColors.success
                        )
                    }
                    attention.forEach { (text, color, target) ->
                        HoverableRow(onClick = { onNavigate(target) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(10.dp))
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                val debtors = receivables
                    .groupBy { it.clientId }
                    .map { (clientId, list) -> clientId to list.sumOf { it.balanceDue } }
                    .sortedByDescending { it.second }
                    .take(5)
                if (debtors.isNotEmpty()) {
                    SectionCard(title = "Хто винен") {
                        debtors.forEach { (clientId, amount) ->
                            HoverableRow(onClick = { onNavigate(Screen.Payments) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        clientById[clientId]?.displayName ?: "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DesktopColors.warning
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard(title = "По статусах") {
                    @Suppress("DEPRECATION")
                    val statusCounts = orders
                        .groupBy { it.status }
                        .toSortedMap(compareBy { it.ordinal })
                    statusCounts.forEach { (status, statusOrders) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status)
                                Text(
                                    statusOrders.size.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { statusOrders.size.toFloat() / orders.size },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                drawStopIndicator = {}
                            )
                        }
                    }
                }
            }
        }
    }


    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Встановити оновлення") },
            text = { Text("Програму буде закрито та запущено інсталятор. Продовжити?") },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallDialog = false
                        onInstallUpdate()
                    }
                ) {
                    Text("Встановити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

private fun buildUpdateProgressText(state: UpdateUiState): String {
    val total = state.totalBytes
    val downloadedMb = state.downloadedBytes / (1024.0 * 1024.0)
    return if (total != null && total > 0L) {
        val percent = ((state.downloadedBytes * 100.0) / total).coerceIn(0.0, 100.0)
        val totalMb = total / (1024.0 * 1024.0)
        "Завантаження… %.0f%% (%.1f / %.1f МБ)".format(percent, downloadedMb, totalMb)
    } else {
        "Завантаження… %.1f МБ".format(downloadedMb)
    }
}
