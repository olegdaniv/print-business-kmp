@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.models.CostBreakdown
import com.printbusinesskmp.models.OrderItem
import com.printbusinesskmp.models.PrintArea
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.MediumGray
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.StatusBackground.Cancelled
import com.printbusinesskmp.theme.AppColors.Success
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils

// Enhanced OrderItemForm with timestamp
data class OrderItemFormEnhanced(
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

@Composable
private fun TooltipIconButton(
    text: String,
    onClick: () -> Unit = {}
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { showTooltip = !showTooltip },
            contentPadding = PaddingValues(4.dp)
        ) {
            Text("ℹ️", fontSize = 14.sp)
        }

        if (showTooltip) {
            Card(
                modifier = Modifier.padding(start = 32.dp).widthIn(max = 250.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = text,
                    color = White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun QuickCalculateAllDialog(
    onDismiss: () -> Unit,
    onApply: (laborMinutes: Int, laborRate: Double, profitMargin: Double) -> Unit
) {
    var laborMinutes by remember { mutableStateOf("15") }
    var laborRate by remember { mutableStateOf("100") }
    var profitMargin by remember { mutableStateOf(50f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡ Quick Calculate All Items")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Apply these default values to all items without prices:",
                    fontSize = 14.sp,
                    color = MediumGray
                )

                OutlinedTextField(
                    value = laborMinutes,
                    onValueChange = { if (it.all { c -> c.isDigit() }) laborMinutes = it },
                    label = { Text("Default Labor Time (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = laborRate,
                    onValueChange = { laborRate = it },
                    label = { Text("Default Labor Rate (грн./hour)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Default Profit Margin:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${profitMargin.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                    }
                    Slider(
                        value = profitMargin,
                        onValueChange = { profitMargin = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        laborMinutes.toIntOrNull() ?: 15,
                        laborRate.toDoubleOrNull() ?: 100.0,
                        profitMargin.toDouble()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Apply to All Uncalculated Items", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AnimatedSuccessBanner(message: String, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
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
                    message,
                    fontSize = 14.sp,
                    color = Success,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AnimatedErrorBanner(message: String?, visible: Boolean) {
    AnimatedVisibility(
        visible = visible && message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Cancelled)
        ) {
            Text(
                text = message ?: "",
                fontSize = 14.sp,
                color = AppColors.Error,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun AnimatedCostBreakdownCard(
    breakdown: CostBreakdown,
    quantity: Int,
    visible: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
        border = BorderStroke(2.dp, PrimaryBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cost Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Costs in RED
            BreakdownRow(
                label = "Total Materials:",
                value = FormatUtils.formatCurrency(breakdown.materialsCost),
                valueColor = AppColors.Error
            )
            BreakdownRow(
                label = "Labor Cost:",
                value = FormatUtils.formatCurrency(breakdown.laborCost),
                valueColor = AppColors.Error
            )
            BreakdownRow(
                label = "Total Cost per item:",
                value = FormatUtils.formatCurrency(breakdown.totalCost / quantity),
                valueColor = AppColors.Error,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Selling Price in BLUE
            BreakdownRow(
                label = "Selling Price per item:",
                value = FormatUtils.formatCurrency(breakdown.finalSellingPrice / quantity),
                valueColor = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )

            // Tax and Profit in GREEN
            BreakdownRow(
                label = "Tax (5%):",
                value = FormatUtils.formatCurrency(breakdown.simplifiedTax),
                valueColor = Success
            )
            BreakdownRow(
                label = "Profit per item:",
                value = FormatUtils.formatCurrency(breakdown.actualProfit / quantity),
                valueColor = Success,
                fontWeight = FontWeight.Bold
            )
            BreakdownRow(
                label = "Profit Margin:",
                value = "${breakdown.profitMarginPercent.toInt()}%",
                valueColor = Success,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = DarkGrayText)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            color = valueColor
        )
    }
}