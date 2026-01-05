package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.*
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.utils.PricingCalculator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingCalculatorScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    var productType by remember { mutableStateOf(ProductType.T_SHIRT) }
    var quantity by remember { mutableStateOf("1") }
    var printArea by remember { mutableStateOf(PrintArea.FRONT) }
    var laborMinutes by remember { mutableStateOf("30") }
    var laborRate by remember { mutableStateOf("100") }
    var profitMargin by remember { mutableStateOf(50f) }
    var costBreakdown by remember { mutableStateOf<CostBreakdown?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var productTypeExpanded by remember { mutableStateOf(false) }
    var printAreaExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun calculatePricing() {
        val quantityInt = quantity.toIntOrNull() ?: return
        val laborMinutesInt = laborMinutes.toIntOrNull() ?: return
        val laborRateDouble = laborRate.toDoubleOrNull() ?: return

        if (quantityInt <= 0 || laborMinutesInt < 0 || laborRateDouble <= 0) {
            errorMessage = "Please enter valid values"
            return
        }

        scope.launch {
            try {
                isCalculating = true
                errorMessage = null

                val request = PricingRequest(
                    productType = productType,
                    quantity = quantityInt,
                    printArea = printArea,
                    laborMinutes = laborMinutesInt,
                    profitMarginPercent = profitMargin.toDouble(),
                    laborRatePerHour = laborRateDouble
                )

                costBreakdown = ApiClient.calculatePricing(request)
                isCalculating = false
            } catch (e: Exception) {
                errorMessage = "Failed to calculate pricing: ${e.message}"
                isCalculating = false
            }
        }
    }

    // Auto-calculate when inputs change
    LaunchedEffect(productType, quantity, printArea, laborMinutes, laborRate, profitMargin) {
        val quantityInt = quantity.toIntOrNull()
        val laborMinutesInt = laborMinutes.toIntOrNull()
        val laborRateDouble = laborRate.toDoubleOrNull()
        if (quantityInt != null && quantityInt > 0 &&
            laborMinutesInt != null && laborMinutesInt >= 0 &&
            laborRateDouble != null && laborRateDouble > 0) {
            calculatePricing()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Pricing Calculator",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column - Inputs
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Product Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Product Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productTypeExpanded,
                        onExpandedChange = { productTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = productType.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = productTypeExpanded,
                            onDismissRequest = { productTypeExpanded = false }
                        ) {
                            ProductType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace("_", " ")) },
                                    onClick = {
                                        productType = type
                                        productTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Quantity Input
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity (min: 1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Print Area Dropdown
                    ExposedDropdownMenuBox(
                        expanded = printAreaExpanded,
                        onExpandedChange = { printAreaExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = printArea.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Print Area") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = printAreaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = printAreaExpanded,
                            onDismissRequest = { printAreaExpanded = false }
                        ) {
                            PrintArea.entries.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.name) },
                                    onClick = {
                                        printArea = area
                                        printAreaExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Labor Time Input
                    OutlinedTextField(
                        value = laborMinutes,
                        onValueChange = { laborMinutes = it },
                        label = { Text("Labor Time (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Labor Rate Input
                    OutlinedTextField(
                        value = laborRate,
                        onValueChange = { laborRate = it },
                        label = { Text("Labor Rate (₴/hour)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Profit Margin Slider (0-100%, step 5%)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Profit Margin: ${profitMargin.roundToInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Slider(
                            value = profitMargin,
                            onValueChange = { profitMargin = it },
                            valueRange = 0f..100f,
                            steps = 19, // Steps for every 5%: (100-0)/5 - 1 = 19
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("50%", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("100%", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Calculate Button
                    Button(
                        onClick = { calculatePricing() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCalculating
                    ) {
                        Text(if (isCalculating) "Calculating..." else "Calculate")
                    }
                }
            }

            // Right Column - Results
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pricing Breakdown",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isCalculating) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (costBreakdown != null) {
                        val breakdown = costBreakdown!!

                        // Cost Summary (Red)
                        CostRow("Materials Cost", breakdown.materialsCost, color = Color(0xFFDC2626))
                        CostRow("Labor Cost", breakdown.laborCost, color = Color(0xFFDC2626))
                        CostRow("Overhead Cost", breakdown.overheadCost, color = Color(0xFFDC2626))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        CostRow("Total Cost", breakdown.totalCost, bold = true, color = Color(0xFFDC2626))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pricing Summary (Blue)
                        CostRow("Desired Profit Margin", "${breakdown.profitMarginPercent.roundToInt()}%", isText = true, color = Color(0xFF2563EB))
                        CostRow("Selling Price (Before Tax)", breakdown.sellingPriceBeforeTax, color = Color(0xFF2563EB))
                        CostRow("Simplified Tax (5%)", breakdown.simplifiedTax, color = Color(0xFF2563EB))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        CostRow(
                            "Final Selling Price",
                            breakdown.finalSellingPrice,
                            bold = true,
                            color = Color(0xFF2563EB)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Profit Summary (Green)
                        CostRow(
                            "Actual Profit",
                            breakdown.actualProfit,
                            bold = true,
                            color = Color(0xFF16A34A)
                        )
                        CostRow(
                            "Profit Margin %",
                            "${(breakdown.desiredProfitMargin * 100).roundToInt()}%",
                            isText = true,
                            color = Color(0xFF16A34A)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Per Item Price
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Price per Item",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    PricingCalculator.formatCurrency(breakdown.finalSellingPrice / quantity.toIntOrNull()!!),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    } else {
                        Text(
                            "Enter product details and click Calculate",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostRow(
    label: String,
    value: Double,
    bold: Boolean = false,
    color: Color = Color(0xFF1E293B),
    isText: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = Color(0xFF64748B)
        )
        Text(
            text = PricingCalculator.formatCurrency(value),
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
private fun CostRow(
    label: String,
    value: String,
    bold: Boolean = false,
    color: Color = Color(0xFF1E293B),
    isText: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}