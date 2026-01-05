@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.*
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class OrderItemForm(
    val id: String = "",
    val productType: ProductType = ProductType.T_SHIRT,
    val quantity: Int = 1,
    val size: String = "",
    val color: String = "",
    val printArea: PrintArea = PrintArea.FRONT,
    val blankItemCost: Double = 0.0,
    val thermalPaperCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val sellingPrice: Double = 0.0
) {
    val totalCost: Double get() = blankItemCost + thermalPaperCost + laborCost
    val profit: Double get() = sellingPrice - totalCost
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
                errorMessage = "Failed to load clients: ${e.message}"
                isLoading = false
            }
        }
    }

    fun handleSave() {
        val clientId = selectedClientId
        if (clientId == null) {
            errorMessage = "Please select a client"
            return
        }

        if (orderItems.isEmpty()) {
            errorMessage = "Please add at least one item"
            return
        }

        scope.launch {
            try {
                isSaving = true
                val now = Clock.System.now()

                val items = orderItems.map { item ->
                    OrderItem.create(
                        id = "",
                        productType = item.productType,
                        quantity = item.quantity,
                        size = item.size.ifBlank { null },
                        color = item.color.ifBlank { null },
                        printArea = item.printArea,
                        blankItemCost = item.blankItemCost,
                        thermalPaperCost = item.thermalPaperCost,
                        laborCost = item.laborCost,
                        sellingPrice = item.sellingPrice
                    )
                }

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
                errorMessage = "Failed to create order: ${e.message}"
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
                text = "New Order",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            TextButton(onClick = { onNavigate(Screen.Orders) }) {
                Text("← Cancel", color = Color(0xFF3B82F6))
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Client Selection",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
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
                                    placeholder = { Text("Select a client") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = true,
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order Items (${orderItems.size})",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )

                                Button(
                                    onClick = { showAddItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) {
                                    Text("+ Add Item", color = Color.White)
                                }
                            }

                            if (orderItems.isEmpty()) {
                                Text(
                                    text = "No items added yet",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                orderItems.forEachIndexed { index, item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.productType.name.replace("_", " "),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E293B)
                                                )

                                                TextButton(
                                                    onClick = {
                                                        orderItems = orderItems.filterIndexed { i, _ -> i != index }
                                                    }
                                                ) {
                                                    Text("Remove", color = Color(0xFFEF4444))
                                                }
                                            }

                                            Text(
                                                text = "Qty: ${item.quantity} | ${item.printArea.name}",
                                                fontSize = 14.sp,
                                                color = Color(0xFF64748B)
                                            )

                                            if (item.size.isNotBlank() || item.color.isNotBlank()) {
                                                Text(
                                                    text = listOfNotNull(
                                                        item.size.takeIf { it.isNotBlank() },
                                                        item.color.takeIf { it.isNotBlank() }
                                                    ).joinToString(", "),
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Cost: ${FormatUtils.formatCurrency(item.totalCost)}",
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                                Text(
                                                    text = "Price: ${FormatUtils.formatCurrency(item.sellingPrice)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Text(
                                                    text = "Profit: ${FormatUtils.formatCurrency(item.profit)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (item.profit >= 0) Color(0xFF16A34A) else Color(
                                                        0xFFEF4444
                                                    )
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
                                            text = "Total Cost:",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.totalCost }),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Total Price:",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.sellingPrice }),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Total Profit:",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(orderItems.sumOf { it.profit }),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (orderItems.sumOf { it.profit } >= 0) Color(0xFF16A34A) else Color(
                                                0xFFEF4444
                                            )
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Notes (Optional)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("Add any additional notes...") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Save Order", color = Color.White, fontSize = 16.sp)
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
    var blankItemCost by remember { mutableStateOf("") }
    var thermalPaperCost by remember { mutableStateOf("") }
    var laborCost by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    val totalCost = (blankItemCost.toDoubleOrNull() ?: 0.0) +
            (thermalPaperCost.toDoubleOrNull() ?: 0.0) +
            (laborCost.toDoubleOrNull() ?: 0.0)
    val profit = (sellingPrice.toDoubleOrNull() ?: 0.0) - totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Order Item") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Product Type
                    Text(
                        text = "Product Type",
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
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
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
                        label = { Text("Quantity") },
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
                            label = { Text("Size") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Color") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Print Area
                    Text(
                        text = "Print Area",
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
                    Text(
                        text = "Costs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = blankItemCost,
                        onValueChange = { blankItemCost = it },
                        label = { Text("Blank Item Cost") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                item {
                    OutlinedTextField(
                        value = thermalPaperCost,
                        onValueChange = { thermalPaperCost = it },
                        label = { Text("Thermal Paper Cost") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                item {
                    OutlinedTextField(
                        value = laborCost,
                        onValueChange = { laborCost = it },
                        label = { Text("Labor Cost") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                item {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling Price") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                item {
                    // Calculated Values
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Cost:", fontSize = 14.sp)
                                Text(FormatUtils.formatCurrency(totalCost), fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Profit:", fontSize = 14.sp)
                                Text(
                                    FormatUtils.formatCurrency(profit),
                                    fontWeight = FontWeight.Medium,
                                    color = if (profit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444)
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
                            blankItemCost = blankItemCost.toDoubleOrNull() ?: 0.0,
                            thermalPaperCost = thermalPaperCost.toDoubleOrNull() ?: 0.0,
                            laborCost = laborCost.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}