@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.CostBreakdown
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderItem
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.PrintArea
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.shared.resources.Res
import com.printbusinesskmp.shared.resources.action_cancel
import com.printbusinesskmp.shared.resources.action_remove
import com.printbusinesskmp.shared.resources.item_add_title
import com.printbusinesskmp.shared.resources.item_color
import com.printbusinesskmp.shared.resources.item_print_area
import com.printbusinesskmp.shared.resources.item_quantity
import com.printbusinesskmp.shared.resources.item_size
import com.printbusinesskmp.shared.resources.label_optional
import com.printbusinesskmp.shared.resources.nav_orders
import com.printbusinesskmp.shared.resources.order_add_item
import com.printbusinesskmp.shared.resources.order_detail_label_client
import com.printbusinesskmp.shared.resources.order_detail_label_profit
import com.printbusinesskmp.shared.resources.order_detail_total_cost
import com.printbusinesskmp.shared.resources.order_form_button_add_item
import com.printbusinesskmp.shared.resources.order_form_label_design_url_optional
import com.printbusinesskmp.shared.resources.order_form_label_labor_time_min
import com.printbusinesskmp.shared.resources.order_form_label_rate_uah_hour
import com.printbusinesskmp.shared.resources.order_form_profit_margin_label
import com.printbusinesskmp.shared.resources.order_items
import com.printbusinesskmp.shared.resources.order_no_items
import com.printbusinesskmp.shared.resources.order_notes
import com.printbusinesskmp.shared.resources.order_save
import com.printbusinesskmp.shared.resources.order_select_client
import com.printbusinesskmp.shared.resources.pricing_blank_cost
import com.printbusinesskmp.shared.resources.pricing_breakdown
import com.printbusinesskmp.shared.resources.pricing_calculate
import com.printbusinesskmp.shared.resources.pricing_calculating
import com.printbusinesskmp.shared.resources.pricing_calculator
import com.printbusinesskmp.shared.resources.pricing_cost_per_item
import com.printbusinesskmp.shared.resources.pricing_labor_cost
import com.printbusinesskmp.shared.resources.pricing_manual_entry
import com.printbusinesskmp.shared.resources.pricing_materials
import com.printbusinesskmp.shared.resources.pricing_profit_margin
import com.printbusinesskmp.shared.resources.pricing_profit_per_item
import com.printbusinesskmp.shared.resources.pricing_selling_price
import com.printbusinesskmp.shared.resources.pricing_selling_price_per_item
import com.printbusinesskmp.shared.resources.pricing_success
import com.printbusinesskmp.shared.resources.pricing_tax
import com.printbusinesskmp.shared.resources.pricing_thermal_cost
import com.printbusinesskmp.shared.resources.summary_cost
import com.printbusinesskmp.shared.resources.summary_hide
import com.printbusinesskmp.shared.resources.summary_price
import com.printbusinesskmp.shared.resources.summary_profit
import com.printbusinesskmp.shared.resources.summary_qty
import com.printbusinesskmp.shared.resources.summary_show
import com.printbusinesskmp.shared.resources.summary_total_cost
import com.printbusinesskmp.shared.resources.summary_total_price
import com.printbusinesskmp.shared.resources.summary_total_profit
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.CardItemBg
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.StatusBackground.Cancelled
import com.printbusinesskmp.theme.AppColors.Success
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

data class OrderItemForm(
    val id: String = "",
    val productType: ProductType = ProductType.T_SHIRT,
    val quantity: Int = 1,
    val size: String = "",
    val color: String = "",
    val printArea: PrintArea = PrintArea.FRONT,
    val designUrl: String = "",
    val blankItemCost: Double = 0.0,
    val thermalPaperCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val laborMinutes: Int = 15,
    val laborRatePerHour: Double = 100.0,
    val profitMarginPercent: Double = 50.0,
    val calculatedBreakdown: CostBreakdown? = null,
    val isCalculated: Boolean = false,
    val calculatedAt: Long? = null
) {
    val totalCost: Double get() = blankItemCost + thermalPaperCost + laborCost
    val profit: Double get() = sellingPrice - totalCost

    fun toOrderItem(): OrderItem = OrderItem.create(
        id = "",
        productType = productType,
        quantity = quantity,
        size = size.ifBlank { null },
        color = color.ifBlank { null },
        printArea = printArea,
        blankItemCost = blankItemCost,
        thermalPaperCost = thermalPaperCost,
        laborCost = laborCost,
        sellingPrice = sellingPrice,
        laborTimeUsed = if (isCalculated) laborMinutes else null,
        laborRateUsed = if (isCalculated) laborRatePerHour else null,
        profitMarginUsed = if (isCalculated) profitMarginPercent else null,
        calculatedAt = calculatedAt
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(onNavigate: (Screen) -> Unit) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var orderItems by remember { mutableStateOf<List<OrderItemForm>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                clients = ApiClient.getClients()
                isLoading = false
            } catch (e: Exception) {
//                errorMessage = "${Strings.failedToLoadClients()}: ${e.message}"
                isLoading = false
            }
        }
    }

    fun handleSave() {
        val clientId = selectedClientId
        if (clientId == null) {
//            errorMessage = Strings.pleaseSelectClient()
            return
        }

        if (orderItems.isEmpty()) {
//            errorMessage = Strings.pleaseAddAtLeastOneItem()
            return
        }

        scope.launch {
            try {
                isSaving = true
                val now = Clock.System.now()

                val items = orderItems.map { item -> item.toOrderItem() }

                val order = Order.create(
                    id = "",
                    clientId = clientId,
                    items = items,
                    status = OrderStatus.NEW,
                    createdAt = now,
                    updatedAt = now,
                    notes = notes.ifBlank { null }
                )

                ApiClient.createOrder(order)
                onNavigate(Screen.Orders)
            } catch (e: Exception) {
//                errorMessage = "${Strings.failedToCreateOrder()}: ${e.message}"
                isSaving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.nav_orders),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkSlate
            )

            TextButton(onClick = { onNavigate(Screen.Orders) }) {
                Text(
                    text = stringResource(Res.string.action_cancel),
                    color = PrimaryBlue
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Client Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(Res.string.order_detail_label_client),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.DarkSlate,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = clients.find { it.id == selectedClientId }?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = {
                                        Text(stringResource(Res.string.order_select_client))
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = expanded
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = true,
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    clients.forEach { client ->
                                        DropdownMenuItem(
                                            text = { Text("${client.name} - ${client.phone}") },
                                            onClick = {
                                                selectedClientId = client.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Order Items Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(Res.string.order_items)} (${orderItems.size})",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.DarkSlate
                                )

                                Button(
                                    onClick = { showAddItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(
                                        "+ ${stringResource(Res.string.order_add_item)}",
                                        color = White
                                    )
                                }
                            }

                            if (orderItems.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.order_no_items),
                                    fontSize = 14.sp,
                                    color = AppColors.MediumGray,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                orderItems.forEachIndexed { index, item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        colors = CardDefaults.cardColors(containerColor = CardItemBg)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.productType.toString(),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppColors.DarkSlate
                                                )

                                                TextButton(
                                                    onClick = {
                                                        orderItems =
                                                            orderItems.filterIndexed { i, _ -> i != index }
                                                    }
                                                ) {
                                                    Text(
                                                        text = stringResource(Res.string.action_remove),
                                                        color = AppColors.Error
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${stringResource(Res.string.summary_qty)}: ${item.quantity} | ${
                                                    item.printArea
                                                }",
                                                fontSize = 14.sp,
                                                color = AppColors.MediumGray
                                            )

                                            if (item.size.isNotBlank() || item.color.isNotBlank()) {
                                                Text(
                                                    text = listOfNotNull(
                                                        item.size.takeIf { it.isNotBlank() },
                                                        item.color.takeIf { it.isNotBlank() }
                                                    ).joinToString(", "),
                                                    fontSize = 14.sp,
                                                    color = AppColors.MediumGray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${stringResource(Res.string.summary_cost)}: ${
                                                        FormatUtils.formatCurrency(
                                                            item.totalCost
                                                        )
                                                    }",
                                                    fontSize = 14.sp,
                                                    color = AppColors.MediumGray
                                                )
                                                Text(
                                                    text = "${stringResource(Res.string.summary_price)}: ${
                                                        FormatUtils.formatCurrency(
                                                            item.sellingPrice
                                                        )
                                                    }",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppColors.DarkSlate
                                                )
                                                Text(
                                                    text = "${stringResource(Res.string.summary_profit)}: ${
                                                        FormatUtils.formatCurrency(
                                                            item.profit
                                                        )
                                                    }",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (item.profit >= 0) Success else AppColors.Error
                                                )
                                            }
                                        }
                                    }
                                }

                                // Order Total
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.summary_total_cost) + ":",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppColors.MediumGray
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.totalCost }),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppColors.DarkSlate
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.summary_total_price) + ":",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppColors.MediumGray
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.sellingPrice }),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppColors.DarkSlate
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.summary_total_profit) + ":",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.DarkSlate
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.profit }),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (orderItems.sumOf { it.profit } >= 0) Success else AppColors.Error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Notes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "${stringResource(Res.string.order_notes)} (${
                                    stringResource(
                                        Res.string.label_optional
                                    )
                                })",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.DarkSlate,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = {
                                    Text(stringResource(Res.string.order_add_item))
                                },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = White,
                                    unfocusedContainerColor = White
                                )
                            )
                        }
                    }
                }

                item {
                    // Error Message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.order_save),
                                color = White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { item ->
                orderItems = orderItems + item
                showAddItemDialog = false
            }
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (OrderItemForm) -> Unit
) {
    var productType by remember { mutableStateOf(ProductType.T_SHIRT) }
    var quantity by remember { mutableStateOf("1") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var printArea by remember { mutableStateOf(PrintArea.FRONT) }
    var designUrl by remember { mutableStateOf("") }
    var blankItemCost by remember { mutableStateOf("") }
    var thermalPaperCost by remember { mutableStateOf("") }
    var laborCost by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    var laborMinutes by remember { mutableStateOf("15") }
    var laborRatePerHour by remember { mutableStateOf("100") }
    var profitMarginPercent by remember { mutableStateOf(50f) }

    var isCalculating by remember { mutableStateOf(false) }
    var calculatedBreakdown by remember { mutableStateOf<CostBreakdown?>(null) }
    var calculationError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var highlightFields by remember { mutableStateOf(false) }
    var showBreakdown by remember { mutableStateOf(false) }
    var calculatedTimestamp by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()

    // Auto-dismiss error after 5 seconds
    LaunchedEffect(calculationError) {
        if (calculationError != null) {
            delay(5000)
            calculationError = null
        }
    }

    val totalCost = (blankItemCost.toDoubleOrNull() ?: 0.0) +
            (thermalPaperCost.toDoubleOrNull() ?: 0.0) +
            (laborCost.toDoubleOrNull() ?: 0.0)
    val profit = (sellingPrice.toDoubleOrNull() ?: 0.0) - totalCost

    fun calculatePricing() {
        scope.launch {
            try {
                isCalculating = true
                calculationError = null
                showSuccess = false

                val request = PricingRequest(
                    productType = productType,
                    quantity = quantity.toIntOrNull() ?: 1,
                    printArea = printArea,
                    laborMinutes = laborMinutes.toIntOrNull() ?: 15,
                    profitMarginPercent = profitMarginPercent.toDouble(),
                    laborRatePerHour = laborRatePerHour.toDoubleOrNull() ?: 100.0
                )

                val breakdown = ApiClient.calculatePricing(request)
                calculatedBreakdown = breakdown
                calculatedTimestamp = Clock.System.now().toEpochMilliseconds()

                val qty = quantity.toIntOrNull() ?: 1
                val thermalCost = if (printArea == PrintArea.BOTH) 10.0 else 5.0
                blankItemCost =
                    (((breakdown.materialsCost / qty - thermalCost) * 100).toInt() / 100.0).toString()
                thermalPaperCost = ((thermalCost * 100).toInt() / 100.0).toString()
                laborCost = ((breakdown.laborCost * 100).toInt() / 100.0).toString()
                sellingPrice =
                    ((breakdown.finalSellingPrice / qty * 100).toInt() / 100.0).toString()

                showSuccess = true
                showBreakdown = true
                highlightFields = true

                delay(3000)
                highlightFields = false

                isCalculating = false
            } catch (e: Exception) {
                calculationError = "Calculation failed: ${e.message}"
                isCalculating = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.item_add_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Product Type
                    Text(
                        text = "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = productType.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth()
                                .menuAnchor(
                                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ProductType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace("_", " ")) },
                                    onClick = {
                                        productType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Quantity
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                        label = { Text(stringResource(Res.string.item_quantity)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = size,
                            onValueChange = { size = it },
                            label = { Text(stringResource(Res.string.item_size)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text(stringResource(Res.string.item_color)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Print Area
                    Text(
                        text = stringResource(Res.string.item_print_area),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = printArea.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = false,
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            PrintArea.entries.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.name) },
                                    onClick = {
                                        printArea = area
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = designUrl,
                        onValueChange = { designUrl = it },
                        label = { Text(stringResource(Res.string.order_form_label_design_url_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(Res.string.pricing_calculator),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = laborMinutes,
                            onValueChange = { if (it.all { c -> c.isDigit() }) laborMinutes = it },
                            label = { Text(stringResource(Res.string.order_form_label_labor_time_min)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = laborRatePerHour,
                            onValueChange = { laborRatePerHour = it },
                            label = { Text(stringResource(Res.string.order_form_label_rate_uah_hour)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                item {
                    Column {
                        Text(
                            text = stringResource(
                                Res.string.order_form_profit_margin_label,
                                profitMarginPercent.toInt().toString()
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Slider(
                            value = profitMarginPercent,
                            onValueChange = { profitMarginPercent = it },
                            valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Button(
                        onClick = { calculatePricing() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCalculating,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isCalculating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.pricing_calculating), color = White)
                        } else {
                            Text(stringResource(Res.string.pricing_calculate), color = White)
                        }
                    }
                }

                if (showSuccess) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppColors.StatusBackground.New)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓", fontSize = 20.sp, color = Success)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(Res.string.pricing_success),
                                    fontSize = 14.sp,
                                    color = Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (calculationError != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Cancelled)
                        ) {
                            Text(
                                text = calculationError ?: "",
                                fontSize = 14.sp,
                                color = AppColors.Error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (showBreakdown && calculatedBreakdown != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppColors.StatusBackground.New)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(Res.string.pricing_breakdown),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                    TextButton(onClick = { showBreakdown = !showBreakdown }) {
                                        Text(
                                            text = if (showBreakdown) {
                                                stringResource(Res.string.summary_hide)
                                            } else stringResource(
                                                Res.string.summary_show
                                            ),
                                            color = Success
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val breakdown = calculatedBreakdown!!
                                val qty = quantity.toIntOrNull() ?: 1

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(resource = Res.string.pricing_materials),
                                        fontSize = 14.sp,
                                        color = Success
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.materialsCost),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Success
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text =
                                            stringResource(Res.string.pricing_labor_cost),
                                        fontSize = 14.sp,
                                        color = Success
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.laborCost),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Success
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(Res.string.pricing_cost_per_item),
                                        fontSize = 14.sp,
                                        color = Success
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.totalCost / qty),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Success
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.pricing_selling_price_per_item),
                                        fontSize = 14.sp,
                                        color = PrimaryBlue
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.finalSellingPrice / qty),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.pricing_tax),
                                        fontSize = 14.sp,
                                        color = Success
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.simplifiedTax),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Success
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.pricing_profit_per_item),
                                        fontSize = 14.sp, color = Success
                                    )
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.actualProfit / qty),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(Res.string.pricing_profit_margin),
                                        fontSize = 14.sp, color = Success
                                    )
                                    Text(
                                        "${breakdown.profitMarginPercent.toInt()}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(Res.string.pricing_manual_entry),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = blankItemCost,
                        onValueChange = { blankItemCost = it },
                        label = { Text(stringResource(Res.string.pricing_blank_cost)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = if (highlightFields) OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFBFDBFE),
                            unfocusedContainerColor = Color(0xFFBFDBFE)
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                }

                item {
                    OutlinedTextField(
                        value = thermalPaperCost,
                        onValueChange = { thermalPaperCost = it },
                        label = { Text(stringResource(Res.string.pricing_thermal_cost)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = if (highlightFields) OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFBFDBFE),
                            unfocusedContainerColor = Color(0xFFBFDBFE)
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                }

                item {
                    OutlinedTextField(
                        value = laborCost,
                        onValueChange = { laborCost = it },
                        label = { Text(stringResource(Res.string.pricing_labor_cost)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = if (highlightFields) OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFBFDBFE),
                            unfocusedContainerColor = Color(0xFFBFDBFE)
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                }

                item {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text(stringResource(Res.string.pricing_selling_price)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = if (highlightFields) OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFBFDBFE),
                            unfocusedContainerColor = Color(0xFFBFDBFE)
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                }

                item {
                    // Calculated Values
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardItemBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(Res.string.order_detail_total_cost),
                                    fontSize = 14.sp
                                )


                                Text(
                                    FormatUtils.formatCurrency(totalCost),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(Res.string.order_detail_label_profit),
                                    fontSize = 14.sp
                                )
                                Text(
                                    FormatUtils.formatCurrency(profit),
                                    fontWeight = FontWeight.Medium,
                                    color = if (profit >= 0) Success else AppColors.Error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        OrderItemForm(
                            productType = productType,
                            quantity = quantity.toIntOrNull() ?: 1,
                            size = size,
                            color = color,
                            printArea = printArea,
                            designUrl = designUrl,
                            blankItemCost = blankItemCost.toDoubleOrNull() ?: 0.0,
                            thermalPaperCost = thermalPaperCost.toDoubleOrNull() ?: 0.0,
                            laborCost = laborCost.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                            laborMinutes = laborMinutes.toIntOrNull() ?: 15,
                            laborRatePerHour = laborRatePerHour.toDoubleOrNull() ?: 100.0,
                            profitMarginPercent = profitMarginPercent.toDouble(),
                            calculatedBreakdown = calculatedBreakdown,
                            isCalculated = calculatedBreakdown != null,
                            calculatedAt = calculatedTimestamp
                        )
                    )
                }
            ) {
                Text(stringResource(Res.string.order_form_button_add_item))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}