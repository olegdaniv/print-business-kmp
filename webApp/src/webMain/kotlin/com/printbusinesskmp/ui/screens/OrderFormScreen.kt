@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderItemDraft
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PricingConfig
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.PricingCalculator
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch

@Composable
fun OrderFormScreen(
    orderId: String? = null,
    onNavigate: (Screen) -> Unit
) {
    val scope = rememberCoroutineScope()
    val editMode = orderId != null

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(OrderStatus.NEW) }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.UNPAID) }
    var notes by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<OrderItemDraft>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showItemDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var removeItemIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(orderId) {
        scope.launch {
            try {
                clients = ApiClient.getClients()
                if (orderId != null) {
                    val order = ApiClient.getOrder(orderId)
                    selectedClientId = order.clientId
                    status = order.status
                    paymentStatus = order.paymentStatus
                    notes = order.notes.orEmpty()
                    items = order.items.map { item ->
                        OrderItemDraft(
                            serviceType = item.serviceType,
                            productType = item.productType,
                            quantity = item.quantity,
                            usedMeters = item.usedMeters,
                            garmentCost = item.garmentCost,
                            pricing = item.pricing,
                            manualPrice = item.manualPrice,
                            notes = item.notes
                        )
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    val totals = items.map { draft -> PricingCalculator.calculate(draft) }
    val totalCost = totals.sumOf { it.totalCost }
    val totalPrice = totals.sumOf { it.finalPrice }
    val totalProfit = totals.sumOf { it.profit }

    Column {
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClientSelector(
                    clients = clients,
                    selectedId = selectedClientId,
                    onSelect = { selectedClientId = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Позиції (${items.size})", fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            editingIndex = null
                            showItemDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        Text("+ Додати", color = AppColors.White)
                    }
                }

                if (items.isEmpty()) {
                    Text("Додайте хоча б одну позицію", color = AppColors.MediumGray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(items) { index, item ->
                            val calc = totals[index]
                            Card(colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${item.serviceType.labelUa()} / ${item.productType.labelUa()}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.DarkSlate
                                    )
                                    Text(
                                        text = "К-сть: ${item.quantity}, метраж: ${FormatUtils.formatDecimal(item.usedMeters)} м",
                                        color = AppColors.MediumGray,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Собівартість: ${FormatUtils.formatCurrency(calc.totalCost)} | Ціна: ${FormatUtils.formatCurrency(calc.finalPrice)} | Прибуток: ${FormatUtils.formatCurrency(calc.profit)}",
                                        color = AppColors.DarkSlate,
                                        fontSize = 13.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                editingIndex = index
                                                showItemDialog = true
                                            }
                                        ) {
                                            Text("Редагувати", color = AppColors.PrimaryBlue)
                                        }
                                        TextButton(
                                            onClick = {
                                                removeItemIndex = index
                                            }
                                        ) {
                                            Text("Видалити", color = AppColors.Error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("Собівартість: ${FormatUtils.formatCurrency(totalCost)}", color = AppColors.DarkSlate)
                Text("Ціна: ${FormatUtils.formatCurrency(totalPrice)}", color = AppColors.DarkSlate)
                Text(
                    "Прибуток: ${FormatUtils.formatCurrency(totalProfit)}",
                    color = if (totalProfit >= 0) AppColors.Success else AppColors.Error,
                    fontWeight = FontWeight.SemiBold
                )
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
                if (items.isEmpty()) {
                    error = "Замовлення не може бути без позицій"
                    return@Button
                }

                val hasInvalidMeters = items.any { it.usedMeters <= 0.0 }
                if (hasInvalidMeters) {
                    error = "У всіх позиціях метраж повинен бути більше нуля"
                    return@Button
                }

                val hasInvalidPrice = items.any { PricingCalculator.calculate(it).finalPrice <= 0.0 }
                if (hasInvalidPrice) {
                    error = "Ціна позиції не може бути нульовою"
                    return@Button
                }

                saving = true
                error = null

                scope.launch {
                    try {
                        if (editMode) {
                            ApiClient.updateOrder(
                                orderId,
                                OrderUpdateRequest(
                                    clientId = clientId,
                                    status = status,
                                    paymentStatus = paymentStatus,
                                    items = items,
                                    notes = notes.ifBlank { null }
                                )
                            )
                        } else {
                            ApiClient.createOrder(
                                OrderCreateRequest(
                                    clientId = clientId,
                                    status = status,
                                    paymentStatus = paymentStatus,
                                    items = items,
                                    notes = notes.ifBlank { null }
                                )
                            )
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

    if (showItemDialog) {
        OrderItemDialog(
            initial = editingIndex?.let { items[it] },
            onDismiss = {
                showItemDialog = false
                editingIndex = null
            },
            onSave = { draft ->
                if (editingIndex == null) {
                    items = items + draft
                } else {
                    items = items.mapIndexed { index, old ->
                        if (index == editingIndex) draft else old
                    }
                }
                showItemDialog = false
                editingIndex = null
            }
        )
    }

    if (removeItemIndex != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeItemIndex = null },
            title = { Text("Видалити позицію") },
            text = { Text("Підтвердьте видалення позиції із замовлення") },
            confirmButton = {
                Button(
                    onClick = {
                        val index = removeItemIndex
                        if (index != null) {
                            items = items.filterIndexed { i, _ -> i != index }
                        }
                        removeItemIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) {
                    Text("Видалити", color = AppColors.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeItemIndex = null }) {
                    Text("Скасувати")
                }
            }
        )
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

@Composable
private fun OrderItemDialog(
    initial: OrderItemDraft?,
    onDismiss: () -> Unit,
    onSave: (OrderItemDraft) -> Unit
) {
    var serviceType by remember { mutableStateOf(initial?.serviceType ?: ServiceType.DTF) }
    var productType by remember { mutableStateOf(initial?.productType ?: ProductType.T_SHIRT) }
    var quantity by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var usedMeters by remember { mutableStateOf(initial?.usedMeters?.toString() ?: "") }
    var garmentCost by remember { mutableStateOf(initial?.garmentCost?.toString() ?: "") }
    var costPerMeter by remember { mutableStateOf(initial?.pricing?.costPerMeter?.toString() ?: "") }
    var overhead by remember { mutableStateOf(initial?.pricing?.overheadPerOrder?.toString() ?: "") }
    var wastePercent by remember { mutableStateOf(initial?.pricing?.wastePercent?.toString() ?: "") }
    var setupFee by remember { mutableStateOf(initial?.pricing?.setupFee?.toString() ?: "") }
    var minPrice by remember { mutableStateOf(initial?.pricing?.minOrderPrice?.toString() ?: "") }
    var margin by remember { mutableStateOf(initial?.pricing?.marginPercent?.toString() ?: "") }
    var taxPercent by remember { mutableStateOf(initial?.pricing?.taxPercent?.toString() ?: "") }
    var manualPrice by remember { mutableStateOf(initial?.manualPrice?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    var formError by remember { mutableStateOf<String?>(null) }

    fun buildDraft(): OrderItemDraft? {
        val parsedQuantity = quantity.toIntOrNull()
        val parsedMeters = usedMeters.toDoubleOrNull()
        val parsedGarmentCost = garmentCost.toDoubleOrNull() ?: 0.0
        val parsedCostPerMeter = costPerMeter.toDoubleOrNull()
        val parsedOverhead = overhead.toDoubleOrNull() ?: 0.0
        val parsedWaste = wastePercent.toDoubleOrNull() ?: 0.0
        val parsedSetup = setupFee.toDoubleOrNull() ?: 0.0
        val parsedMin = minPrice.toDoubleOrNull()
        val parsedMargin = margin.toDoubleOrNull() ?: 0.0
        val parsedTax = taxPercent.toDoubleOrNull()
        val parsedManual = manualPrice.toDoubleOrNull()

        if (parsedQuantity == null || parsedQuantity <= 0) {
            formError = "Кількість повинна бути більше нуля"
            return null
        }
        if (parsedMeters == null || parsedMeters <= 0.0) {
            formError = "Метраж повинен бути більше нуля"
            return null
        }
        if (parsedCostPerMeter == null || parsedCostPerMeter < 0.0) {
            formError = "Вкажіть коректну собівартість за метр"
            return null
        }
        if (parsedMin == null || parsedMin <= 0.0) {
            formError = "Мінімальна ціна повинна бути більше нуля"
            return null
        }
        if (parsedManual != null && parsedManual <= 0.0) {
            formError = "Ручна ціна повинна бути більше нуля"
            return null
        }

        val draft = OrderItemDraft(
            serviceType = serviceType,
            productType = productType,
            quantity = parsedQuantity,
            usedMeters = parsedMeters,
            garmentCost = parsedGarmentCost,
            pricing = PricingConfig(
                costPerMeter = parsedCostPerMeter,
                overheadPerOrder = parsedOverhead,
                wastePercent = parsedWaste,
                setupFee = parsedSetup,
                minOrderPrice = parsedMin,
                marginPercent = parsedMargin,
                taxPercent = parsedTax
            ),
            manualPrice = parsedManual,
            notes = notes.ifBlank { null }
        )

        val result = PricingCalculator.calculate(draft)
        if (result.finalPrice <= 0.0) {
            formError = "Ціна позиції не може бути нульовою"
            return null
        }

        return draft
    }

    val preview = runCatching {
        val parsedMeters = usedMeters.toDoubleOrNull()
        val parsedCostPerMeter = costPerMeter.toDoubleOrNull()
        val parsedMin = minPrice.toDoubleOrNull()
        if (parsedMeters != null && parsedCostPerMeter != null && parsedMin != null) {
            PricingCalculator.calculate(
                PricingRequest(
                    usedMeters = parsedMeters,
                    costPerMeter = parsedCostPerMeter,
                    garmentCost = garmentCost.toDoubleOrNull() ?: 0.0,
                    overheadPerOrder = overhead.toDoubleOrNull() ?: 0.0,
                    wastePercent = wastePercent.toDoubleOrNull() ?: 0.0,
                    setupFee = setupFee.toDoubleOrNull() ?: 0.0,
                    minOrderPrice = parsedMin,
                    marginPercent = margin.toDoubleOrNull() ?: 0.0,
                    taxPercent = taxPercent.toDoubleOrNull(),
                    manualPrice = manualPrice.toDoubleOrNull()
                )
            )
        } else {
            null
        }
    }.getOrNull()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Нова позиція" else "Редагування позиції") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    EnumSelector(
                        label = "Сервіс",
                        values = ServiceType.entries,
                        selected = serviceType,
                        onSelect = { serviceType = it },
                        textMapper = { it.labelUa() }
                    )
                }
                item {
                    EnumSelector(
                        label = "Виріб",
                        values = ProductType.entries,
                        selected = productType,
                        onSelect = { productType = it },
                        textMapper = { it.labelUa() }
                    )
                }
                item { OutlinedTextField(quantity, { quantity = it }, label = { Text("Кількість") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(usedMeters, { usedMeters = it }, label = { Text("Використано метрів") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(garmentCost, { garmentCost = it }, label = { Text("Вартість виробу") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(costPerMeter, { costPerMeter = it }, label = { Text("Собівартість за метр") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(overhead, { overhead = it }, label = { Text("Накладні витрати") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(wastePercent, { wastePercent = it }, label = { Text("Брак %") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(setupFee, { setupFee = it }, label = { Text("Підготовка") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(minPrice, { minPrice = it }, label = { Text("Мінімальна ціна") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(margin, { margin = it }, label = { Text("Маржа %") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(taxPercent, { taxPercent = it }, label = { Text("Податок % (опціонально)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(manualPrice, { manualPrice = it }, label = { Text("Ручна ціна (опціонально)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Примітки") }, modifier = Modifier.fillMaxWidth()) }

                preview?.let { result ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Собівартість: ${FormatUtils.formatCurrency(result.totalCost)}")
                                Text("Рекомендована ціна: ${FormatUtils.formatCurrency(result.suggestedPrice)}")
                                Text("Фінальна ціна: ${FormatUtils.formatCurrency(result.finalPrice)}")
                                Text(
                                    "Прибуток: ${FormatUtils.formatCurrency(result.profit)}",
                                    color = if (result.profit >= 0) AppColors.Success else AppColors.Error
                                )
                            }
                        }
                    }
                }

                if (formError != null) {
                    item {
                        Text(formError ?: "", color = Color.Red)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val draft = buildDraft()
                    if (draft != null) {
                        onSave(draft)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
            ) {
                Text("Зберегти", color = AppColors.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
