package com.printbusinesskmp.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.desktop.api.DesktopApiClient
import com.printbusinesskmp.desktop.platform.choosePdfSavePath
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopApp() {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var clientsById by remember { mutableStateOf<Map<String, Client>>(emptyMap()) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var selectedInvoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }

    var loadingOrders by remember { mutableStateOf(true) }
    var loadingDetails by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }

    var errorDialog by remember { mutableStateOf<String?>(null) }
    var infoDialog by remember { mutableStateOf<String?>(null) }

    suspend fun loadOrders(manualRefresh: Boolean = false) {
        if (manualRefresh) {
            refreshing = true
        } else {
            loadingOrders = true
        }

        try {
            val loadedClients = DesktopApiClient.getClients()
            val loadedOrders = DesktopApiClient.getOrders().sortedByDescending { it.updatedAt }
            val previousSelection = selectedOrderId

            clientsById = loadedClients.associateBy { it.id }
            orders = loadedOrders
            selectedOrderId = when {
                loadedOrders.isEmpty() -> null
                previousSelection != null && loadedOrders.any { it.id == previousSelection } -> previousSelection
                else -> loadedOrders.first().id
            }
        } catch (e: Exception) {
            errorDialog = e.message ?: "Не вдалося завантажити список замовлень"
        } finally {
            loadingOrders = false
            refreshing = false
        }
    }

    suspend fun loadSelectedOrder(orderId: String) {
        loadingDetails = true
        try {
            selectedOrder = DesktopApiClient.getOrder(orderId)
            selectedInvoices = DesktopApiClient.getInvoicesByOrderId(orderId).sortedByDescending { it.issuedAt }
        } catch (e: Exception) {
            errorDialog = e.message ?: "Не вдалося завантажити деталі замовлення"
            selectedOrder = null
            selectedInvoices = emptyList()
        } finally {
            loadingDetails = false
        }
    }

    suspend fun saveInvoicePdf(invoice: Invoice) {
        processing = true
        try {
            val destination = choosePdfSavePath("${invoice.number}.pdf") ?: return
            val pdfBytes = DesktopApiClient.downloadInvoicePdf(invoice.id)

            withContext(Dispatchers.IO) {
                destination.parent?.let { parent -> Files.createDirectories(parent) }
                Files.write(destination, pdfBytes)
            }

            infoDialog = "PDF збережено: $destination"
        } catch (e: Exception) {
            errorDialog = e.message ?: "Не вдалося зберегти PDF"
        } finally {
            processing = false
        }
    }

    fun quickSaveLatestInvoice() {
        val latestInvoice = selectedInvoices.firstOrNull()
        if (latestInvoice == null) {
            infoDialog = "Для цього замовлення ще немає рахунків."
            return
        }
        scope.launch {
            saveInvoicePdf(latestInvoice)
        }
    }

    LaunchedEffect(Unit) {
        loadOrders()
    }

    LaunchedEffect(selectedOrderId) {
        val orderId = selectedOrderId
        if (orderId == null) {
            selectedOrder = null
            selectedInvoices = emptyList()
        } else {
            loadSelectedOrder(orderId)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LightGray)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                when {
                    keyEvent.key == Key.Escape && (errorDialog != null || infoDialog != null) -> {
                        errorDialog = null
                        infoDialog = null
                        true
                    }
                    keyEvent.key == Key.S && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) -> {
                        quickSaveLatestInvoice()
                        true
                    }
                    else -> false
                }
            }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(430.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = AppColors.White)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Замовлення",
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            color = AppColors.DarkSlate
                        )
                        OutlinedButton(
                            onClick = { scope.launch { loadOrders(manualRefresh = true) } },
                            enabled = !loadingOrders && !refreshing
                        ) {
                            Text("Оновити")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (loadingOrders) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (orders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Замовлень поки немає", color = AppColors.MediumGray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(orders, key = { it.id }) { order ->
                                val selected = order.id == selectedOrderId
                                val rowColor = if (selected) {
                                    AppColors.CardItemBg
                                } else {
                                    Color.Transparent
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowColor, RoundedCornerShape(10.dp))
                                        .clickable { selectedOrderId = order.id }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "#${order.id.take(8)}",
                                        color = AppColors.DarkSlate,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = clientsById[order.clientId]?.displayName ?: "Невідомий клієнт",
                                        color = AppColors.MediumGray,
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${order.status.labelUa()} • ${order.paymentStatus.labelUa()}",
                                        color = AppColors.DarkGrayText,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Сума: ${FormatUtils.formatCurrency(order.totalPrice)}",
                                        color = AppColors.DarkSlate,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Прибуток: ${FormatUtils.formatCurrency(order.profit)}",
                                        color = if (order.profit >= 0) AppColors.Success else AppColors.Error,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = AppColors.White)
            ) {
                if (selectedOrderId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Оберіть замовлення зі списку.", color = AppColors.MediumGray)
                    }
                } else if (loadingDetails) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val order = selectedOrder
                    if (order == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Не вдалося завантажити замовлення.", color = AppColors.Error)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Замовлення #${order.id.take(8)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = AppColors.DarkSlate
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        processing = true
                                        scope.launch {
                                            try {
                                                DesktopApiClient.generateInvoice(order.id)
                                                loadSelectedOrder(order.id)
                                                loadOrders(manualRefresh = true)
                                                infoDialog = "Рахунок успішно згенеровано."
                                            } catch (e: Exception) {
                                                errorDialog = e.message ?: "Не вдалося згенерувати рахунок"
                                            } finally {
                                                processing = false
                                            }
                                        }
                                    },
                                    enabled = !processing,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                                ) {
                                    Text("Згенерувати рахунок", color = AppColors.White)
                                }

                                OutlinedButton(
                                    onClick = { quickSaveLatestInvoice() },
                                    enabled = !processing && selectedInvoices.isNotEmpty()
                                ) {
                                    Text("Зберегти PDF (Ctrl/Cmd+S)")
                                }

                                OutlinedButton(
                                    onClick = { scope.launch { loadSelectedOrder(order.id) } },
                                    enabled = !processing
                                ) {
                                    Text("Оновити")
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Клієнт: ${clientsById[order.clientId]?.displayName ?: order.clientId}",
                                        color = AppColors.DarkSlate
                                    )
                                    Text("Статус: ${order.status.labelUa()}", color = AppColors.DarkGrayText)
                                    Text("Оплата: ${order.paymentStatus.labelUa()}", color = AppColors.DarkGrayText)
                                    Text("Створено: ${FormatUtils.formatDateTime(order.createdAt)}", color = AppColors.MediumGray)
                                    Text("Оновлено: ${FormatUtils.formatDateTime(order.updatedAt)}", color = AppColors.MediumGray)
                                    Text("Собівартість: ${FormatUtils.formatCurrency(order.totalCost)}", color = AppColors.DarkSlate)
                                    Text("Ціна: ${FormatUtils.formatCurrency(order.totalPrice)}", color = AppColors.DarkSlate)
                                    Text(
                                        "Прибуток: ${FormatUtils.formatCurrency(order.profit)}",
                                        color = if (order.profit >= 0) AppColors.Success else AppColors.Error
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = AppColors.White)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Позиції (${order.items.size})", fontWeight = FontWeight.SemiBold, color = AppColors.DarkSlate)
                                    order.items.forEach { item ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                                Text(
                                                    "${item.serviceType.labelUa()} / ${item.productType.labelUa()}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppColors.DarkSlate
                                                )
                                                Text(
                                                    "К-сть: ${item.quantity} • Метри: ${FormatUtils.formatDecimal(item.usedMeters)}",
                                                    color = AppColors.MediumGray,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    "Ціна: ${FormatUtils.formatCurrency(item.price)} • Прибуток: ${FormatUtils.formatCurrency(item.profit)}",
                                                    color = AppColors.DarkGrayText,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = AppColors.White)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Рахунки (${selectedInvoices.size})", fontWeight = FontWeight.SemiBold, color = AppColors.DarkSlate)
                                    if (selectedInvoices.isEmpty()) {
                                        Text("Рахунків для цього замовлення ще немає.", color = AppColors.MediumGray)
                                    } else {
                                        selectedInvoices.forEach { invoice ->
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(invoice.number, color = AppColors.DarkSlate, fontWeight = FontWeight.Medium)
                                                        Text(
                                                            "${FormatUtils.formatDate(invoice.issuedAt)} • ${FormatUtils.formatCurrency(invoice.totalAmount)}",
                                                            color = AppColors.MediumGray,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch { saveInvoicePdf(invoice) }
                                                        },
                                                        enabled = !processing
                                                    ) {
                                                        Text("Зберегти PDF")
                                                    }
                                                }
                                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    errorDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { errorDialog = null },
            title = { Text("Помилка") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorDialog = null }) {
                    Text("ОК")
                }
            }
        )
    }

    infoDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text("Інформація") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) {
                    Text("ОК")
                }
            }
        )
    }
}
