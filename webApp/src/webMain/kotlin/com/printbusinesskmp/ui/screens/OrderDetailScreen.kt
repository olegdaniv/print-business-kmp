@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import kotlinx.browser.window
import kotlinx.coroutines.launch

@Composable
fun OrderDetailScreen(orderId: String, onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var order by remember { mutableStateOf<Order?>(null) }
    var client by remember { mutableStateOf<Client?>(null) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    var selectedStatus by remember { mutableStateOf(OrderStatus.DRAFT) }
    var selectedPayment by remember { mutableStateOf(PaymentStatus.UNPAID) }

    var confirmDelete by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val loadedOrder = ApiClient.getOrder(orderId)
                order = loadedOrder
                selectedStatus = loadedOrder.status
                selectedPayment = loadedOrder.paymentStatus
                client = ApiClient.getClient(loadedOrder.clientId)
                invoices = ApiClient.getInvoicesByOrderId(orderId)
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(orderId) { reload() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Замовлення #${orderId.take(8)}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = AppColors.DarkSlate)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onNavigate(Screen.OrderForm(orderId)) }) {
                    Text("Редагувати")
                }
                TextButton(onClick = { onNavigate(Screen.Orders) }) {
                    Text("Назад")
                }
            }
        }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        val current = order ?: return@Column

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Клієнт: ${client?.displayName ?: "-"}")
                Text("Створено: ${FormatUtils.formatDateTime(current.createdAt)}")
                Text("Оновлено: ${FormatUtils.formatDateTime(current.updatedAt)}")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    EnumField(
                        label = "Статус",
                        values = OrderStatus.entries,
                        selected = selectedStatus,
                        onSelect = { selectedStatus = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.weight(1f)
                    )
                    EnumField(
                        label = "Оплата",
                        values = PaymentStatus.entries,
                        selected = selectedPayment,
                        onSelect = { selectedPayment = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            processing = true
                            scope.launch {
                                try {
                                    ApiClient.updateOrderState(
                                        id = orderId,
                                        status = selectedStatus,
                                        paymentStatus = selectedPayment
                                    )
                                    info = "Статус оновлено"
                                    reload()
                                } catch (e: Exception) {
                                    error = e.message ?: "Не вдалося оновити статус"
                                } finally {
                                    processing = false
                                }
                            }
                        },
                        enabled = !processing,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        Text("Оновити", color = AppColors.White)
                    }
                }

                current.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    Text("Примітки: $note", color = AppColors.MediumGray)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Позиції", fontWeight = FontWeight.SemiBold)
                current.items.forEach { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${item.serviceType.labelUa()} / ${item.productType.labelUa()}", fontWeight = FontWeight.SemiBold)
                            Text("К-сть: ${item.quantity}, метри: ${FormatUtils.formatDecimal(item.usedMeters)}", fontSize = 13.sp)
                            Text("Собівартість: ${FormatUtils.formatCurrency(item.cost)}", fontSize = 13.sp)
                            Text("Ціна: ${FormatUtils.formatCurrency(item.price)}", fontSize = 13.sp)
                            Text(
                                "Прибуток: ${FormatUtils.formatCurrency(item.profit)}",
                                color = if (item.profit >= 0) AppColors.Success else AppColors.Error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                HorizontalDivider()
                Text("Собівартість: ${FormatUtils.formatCurrency(current.totalCost)}")
                Text("Ціна: ${FormatUtils.formatCurrency(current.totalPrice)}")
                Text(
                    "Прибуток: ${FormatUtils.formatCurrency(current.profit)}",
                    color = if (current.profit >= 0) AppColors.Success else AppColors.Error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Рахунки", fontWeight = FontWeight.SemiBold)

                Button(
                    onClick = {
                        processing = true
                        scope.launch {
                            try {
                                ApiClient.generateInvoice(orderId)
                                info = "Рахунок згенеровано"
                                reload()
                            } catch (e: Exception) {
                                error = e.message ?: "Не вдалося згенерувати рахунок"
                            } finally {
                                processing = false
                            }
                        }
                    },
                    enabled = !processing,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success)
                ) {
                    Text("Згенерувати рахунок", color = AppColors.White)
                }

                if (invoices.isEmpty()) {
                    Text("Рахунків поки немає", color = AppColors.MediumGray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(invoices) { invoice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${invoice.number} • ${FormatUtils.formatDate(invoice.issuedAt)}")
                                Row {
                                    TextButton(onClick = {
                                        window.open(ApiClient.getInvoiceDownloadUrl(invoice.id), "_blank")
                                    }) {
                                        Text("PDF", color = AppColors.PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Видалення замовлення", color = AppColors.Error, fontWeight = FontWeight.SemiBold)
                    Text("Дія незворотна", color = AppColors.MediumGray, fontSize = 13.sp)
                }
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) {
                    Text("Видалити", color = AppColors.White)
                }
            }
        }

        if (info != null) {
            Text(info ?: "", color = AppColors.Success, modifier = Modifier.padding(top = 8.dp))
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Підтвердження") },
                text = { Text("Видалити замовлення без можливості відновлення?") },
                confirmButton = {
                    Button(
                        onClick = {
                            processing = true
                            scope.launch {
                                try {
                                    ApiClient.deleteOrder(orderId)
                                    confirmDelete = false
                                    onNavigate(Screen.Orders)
                                } catch (e: Exception) {
                                    error = e.message ?: "Помилка видалення"
                                    processing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                    ) {
                        Text("Видалити", color = AppColors.White)
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
}

@Composable
private fun <T> EnumField(
    label: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    textMapper: (T) -> String = { value -> value.toString() },
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$label: ${textMapper(selected)}")
            TextButton(onClick = { expanded = true }) {
                Text("Змінити")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(textMapper(value)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
