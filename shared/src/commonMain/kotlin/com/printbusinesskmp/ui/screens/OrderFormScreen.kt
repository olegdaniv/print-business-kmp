@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.GarmentSource
import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderItemDraft
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PricingConfig
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.SavedItem
import com.printbusinesskmp.models.SavedItemBulkUpsertRequest
import com.printbusinesskmp.models.SavedItemCreateRequest
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch

private data class LineRow(
    val name: String = "",
    val unit: String = "шт.",
    val quantity: String = "1",
    val unitPrice: String = ""
)

private fun lineTotal(row: LineRow): Double {
    val qty = row.quantity.toIntOrNull() ?: 0
    val price = row.unitPrice.toDoubleOrNull() ?: 0.0
    return qty * price
}

@Composable
fun OrderFormScreen(
    orderId: String? = null,
    onNavigate: (Screen) -> Unit
) {
    val scope = rememberCoroutineScope()
    val editMode = orderId != null

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var savedItems by remember { mutableStateOf<List<SavedItem>>(emptyList()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(OrderStatus.DRAFT) }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.UNPAID) }
    var notes by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(listOf(LineRow())) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        scope.launch {
            try {
                clients = ApiClient.getClients()
                savedItems = runCatching { ApiClient.getSavedItems() }.getOrDefault(emptyList())

                if (orderId != null) {
                    val order = ApiClient.getOrder(orderId)
                    selectedClientId = order.clientId
                    status = order.status
                    paymentStatus = order.paymentStatus
                    notes = order.notes.orEmpty()
                    rows = order.items.map { item ->
                        val total = item.manualPrice ?: item.price
                        val unitPrice = if (item.quantity > 0) total / item.quantity else total
                        LineRow(
                            name = item.name
                                ?: "${item.serviceType.labelUa()} / ${item.productType.labelUa()}",
                            unit = item.unit,
                            quantity = item.quantity.toString(),
                            unitPrice = FormatUtils.formatDecimal(unitPrice)
                        )
                    }.takeIf { it.isNotEmpty() } ?: listOf(LineRow())
                }
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    val totalPrice = rows.sumOf { lineTotal(it) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (editMode) "Редагування замовлення" else "Нове замовлення",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkSlate
            )
            TextButton(onClick = { onNavigate(Screen.Orders) }) {
                Text("Скасувати")
            }
        }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClientSelector(
                    clients = clients,
                    selectedId = selectedClientId,
                    onSelect = { selectedClientId = it }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EnumSelector(
                        label = "Статус",
                        values = OrderStatus.entries,
                        selected = status,
                        onSelect = { status = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.weight(1f)
                    )
                    EnumSelector(
                        label = "Оплата",
                        values = PaymentStatus.entries,
                        selected = paymentStatus,
                        onSelect = { paymentStatus = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примітки") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Позиції (${rows.size})", fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { rows = rows + LineRow() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        Text("+ Додати рядок", color = AppColors.White)
                    }
                }

                // Column headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Назва",
                        modifier = Modifier.weight(3f),
                        fontSize = 12.sp,
                        color = AppColors.MediumGray
                    )
                    Text(
                        "Од.",
                        modifier = Modifier.weight(0.7f),
                        fontSize = 12.sp,
                        color = AppColors.MediumGray
                    )
                    Text(
                        "К-сть",
                        modifier = Modifier.weight(0.8f),
                        fontSize = 12.sp,
                        color = AppColors.MediumGray
                    )
                    Text(
                        "Ціна",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = AppColors.MediumGray
                    )
                    Text(
                        "Сума",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = AppColors.MediumGray
                    )
                    Spacer(Modifier.width(36.dp))
                }

                HorizontalDivider()

                rows.forEachIndexed { index, row ->
                    OrderLineRow(
                        row = row,
                        savedItems = savedItems,
                        onRowChange = { updated ->
                            rows = rows.mapIndexed { i, r -> if (i == index) updated else r }
                        },
                        onRemove = {
                            if (rows.size > 1) {
                                rows = rows.filterIndexed { i, _ -> i != index }
                            }
                        }
                    )
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Разом: ${FormatUtils.formatCurrency(totalPrice)}",
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.DarkSlate
                    )
                }
            }
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = {
                val clientId = selectedClientId
                if (clientId == null) {
                    error = "Оберіть клієнта"
                    return@Button
                }
                val validRows = rows.filter { it.name.isNotBlank() }
                if (validRows.isEmpty()) {
                    error = "Додайте хоча б одну позицію з назвою"
                    return@Button
                }
                val invalidPrice = validRows.any { (it.unitPrice.toDoubleOrNull() ?: 0.0) <= 0.0 }
                if (invalidPrice) {
                    error = "Ціна кожної позиції повинна бути більше нуля"
                    return@Button
                }
                val invalidQty = validRows.any { (it.quantity.toIntOrNull() ?: 0) <= 0 }
                if (invalidQty) {
                    error = "Кількість кожної позиції повинна бути більше нуля"
                    return@Button
                }

                saving = true
                error = null

                scope.launch {
                    try {
                        val drafts = validRows.map { row ->
                            val qty = row.quantity.toInt()
                            val unitPrice = row.unitPrice.toDouble()
                            val total = qty * unitPrice
                            OrderItemDraft(
                                serviceType = ServiceType.DTF,
                                productType = ProductType.OTHER,
                                quantity = qty,
                                usedMeters = qty.toDouble(),
                                garmentCost = 0.0,
                                garmentSource = GarmentSource.OUR_STOCK,
                                pricing = PricingConfig(
                                    costPerMeter = 0.0,
                                    overheadPerOrder = 0.0,
                                    wastePercent = 0.0,
                                    setupFee = 0.0,
                                    minOrderPrice = total,
                                    marginPercent = 0.0,
                                    taxPercent = null
                                ),
                                manualPrice = total,
                                name = row.name.trim(),
                                unit = row.unit.trim().ifBlank { "шт." }
                            )
                        }

                        if (editMode) {
                            ApiClient.updateOrder(
                                orderId,
                                OrderUpdateRequest(
                                    clientId = clientId,
                                    status = status,
                                    paymentStatus = paymentStatus,
                                    items = drafts,
                                    notes = notes.ifBlank { null }
                                )
                            )
                        } else {
                            ApiClient.createOrder(
                                OrderCreateRequest(
                                    clientId = clientId,
                                    status = status,
                                    paymentStatus = paymentStatus,
                                    items = drafts,
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }

                        // Auto-save new item names to SavedItems catalog
                        val existingNames = savedItems.map { it.name.lowercase() }.toSet()
                        val newNames = validRows
                            .filter { it.name.trim().lowercase() !in existingNames }
                            .map { row ->
                                SavedItemCreateRequest(
                                    name = row.name.trim(),
                                    unit = row.unit.trim().ifBlank { "шт." },
                                    defaultPrice = row.unitPrice.toDoubleOrNull() ?: 0.0
                                )
                            }
                            .distinctBy { it.name.lowercase() }
                        if (newNames.isNotEmpty()) {
                            runCatching {
                                ApiClient.bulkUpsertSavedItems(SavedItemBulkUpsertRequest(newNames))
                            }
                        }

                        onNavigate(Screen.Orders)
                    } catch (e: Exception) {
                        error = e.message ?: "Помилка збереження"
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) {
                CircularProgressIndicator(color = AppColors.White, strokeWidth = 2.dp)
            } else {
                Text("Зберегти замовлення", color = AppColors.White)
            }
        }
    }
}

@Composable
private fun OrderLineRow(
    row: LineRow,
    savedItems: List<SavedItem>,
    onRowChange: (LineRow) -> Unit,
    onRemove: () -> Unit
) {
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = if (row.name.isNotEmpty() && showSuggestions) {
        savedItems
            .filter { it.name.lowercase().contains(row.name.lowercase()) }
            .take(8)
    } else emptyList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Назва with autocomplete
        Box(modifier = Modifier.weight(3f)) {
            OutlinedTextField(
                value = row.name,
                onValueChange = { newName ->
                    onRowChange(row.copy(name = newName))
                },
                label = { Text("Назва") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            DropdownMenu(
                expanded = suggestions.isNotEmpty(),
                onDismissRequest = { }
            ) {
                suggestions.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.name, fontSize = 13.sp)
                                Text(
                                    "${item.unit} · ${FormatUtils.formatCurrency(item.defaultPrice)}",
                                    fontSize = 11.sp,
                                    color = AppColors.MediumGray
                                )
                            }
                        },
                        onClick = {
                            onRowChange(
                                row.copy(
                                    name = item.name,
                                    unit = item.unit,
                                    unitPrice = FormatUtils.formatDecimal(item.defaultPrice)
                                )
                            )
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = row.unit,
            onValueChange = { onRowChange(row.copy(unit = it)) },
            label = { Text("Од.") },
            modifier = Modifier.weight(0.7f),
            singleLine = true
        )

        OutlinedTextField(
            value = row.quantity,
            onValueChange = { onRowChange(row.copy(quantity = it)) },
            label = { Text("К-сть") },
            modifier = Modifier.weight(0.8f),
            singleLine = true
        )

        OutlinedTextField(
            value = row.unitPrice,
            onValueChange = { onRowChange(row.copy(unitPrice = it)) },
            label = { Text("Ціна") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        val total = lineTotal(row)
        OutlinedTextField(
            value = if (total > 0) FormatUtils.formatDecimal(total) else "",
            onValueChange = {},
            label = { Text("Сума") },
            modifier = Modifier.weight(1f),
            readOnly = true,
            singleLine = true
        )

        TextButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("✕", color = AppColors.Error)
        }
    }
}

@Composable
private fun ClientSelector(
    clients: List<Client>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Клієнт: ${clients.find { it.id == selectedId }?.displayName ?: "-"}")
            TextButton(onClick = { expanded = true }) {
                Text("Обрати")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            clients.forEach { client ->
                DropdownMenuItem(
                    text = { Text("${client.displayName} (${client.phone})") },
                    onClick = {
                        onSelect(client.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> EnumSelector(
    label: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    textMapper: (T) -> String,
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
            values.forEach { option ->
                DropdownMenuItem(
                    text = { Text(textMapper(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
