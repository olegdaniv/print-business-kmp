package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.CardItemBg
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.MediumGray
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.StatusBackground
import com.printbusinesskmp.theme.AppColors.StatusBackground.Ready
import com.printbusinesskmp.theme.AppColors.StatusText
import com.printbusinesskmp.theme.AppColors.Success
import com.printbusinesskmp.theme.AppColors.VeryLightBluGray
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun OrderDetailScreen(
    orderId: String,
    onNavigate: (Screen) -> Unit
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var client by remember { mutableStateOf<Client?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadOrder() {
        scope.launch {
            try {
                isLoading = true
                order = ApiClient.getOrder(orderId)
                order?.let {
                    client = ApiClient.getClient(it.clientId)
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load order: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        loadOrder()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order Details",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )

            TextButton(onClick = { onNavigate(Screen.Orders) }) {
                Text("← Back to Orders", color = PrimaryBlue)
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                fontSize = 16.sp
            )
        }

        val currentOrder = order
        if (currentOrder != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Order Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Order Information",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            InfoRow("Order ID", "#${currentOrder.id}")
                            InfoRow("Client", client?.name ?: "Unknown")
                            InfoRow("Status", "")
                            Box(modifier = Modifier.padding(start = 120.dp, top = 4.dp)) {
                                StatusBadge(status = currentOrder.status.name)
                            }
                            InfoRow("Created", FormatUtils.formatDateTime(currentOrder.createdAt))
                            InfoRow("Updated", FormatUtils.formatDateTime(currentOrder.updatedAt))
                            currentOrder.completedAt?.let {
                                InfoRow("Completed", FormatUtils.formatDateTime(it))
                            }

                            if (currentOrder.notes != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Notes:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MediumGray
                                )
                                Text(
                                    text = currentOrder.notes ?: "",
                                    fontSize = 14.sp,
                                    color = DarkSlate,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Status Update
                            if (currentOrder.status != OrderStatus.COMPLETED && currentOrder.status != OrderStatus.CANCELLED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Update Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MediumGray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OrderStatus.entries.forEach { status ->
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        ApiClient.updateOrderStatus(orderId, status)
                                                        loadOrder()
                                                    } catch (e: Exception) {
                                                        errorMessage = "Failed to update status: ${e.message}"
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (status == currentOrder.status) PrimaryBlue else Color(
                                                    0xFFE2E8F0
                                                )
                                            )
                                        ) {
                                            Text(
                                                text = status.name.replace("_", " "),
                                                color = if (status == currentOrder.status) White else Color(
                                                    0xFF64748B
                                                ),
                                                fontSize = 12.sp
                                            )
                                        }
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
                            Text(
                                text = "Order Items (${currentOrder.items.size})",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            currentOrder.items.forEach { item ->
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
                                                text = item.productType.name.replace("_", " "),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkSlate
                                            )
                                            Text(
                                                text = "Qty: ${item.quantity}",
                                                fontSize = 14.sp,
                                                color = MediumGray
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (item.size != null || item.color != null) {
                                            Text(
                                                text = listOfNotNull(item.size, item.color).joinToString(", "),
                                                fontSize = 14.sp,
                                                color = MediumGray
                                            )
                                        }

                                        Text(
                                            text = "Print Area: ${item.printArea.name}",
                                            fontSize = 14.sp,
                                            color = MediumGray
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Costs",
                                                    fontSize = 12.sp,
                                                    color = MediumGray
                                                )
                                                Text(
                                                    text = "Blank: ${FormatUtils.formatCurrency(item.blankItemCost)}",
                                                    fontSize = 12.sp,
                                                    color = DarkSlate
                                                )
                                                Text(
                                                    text = "Paper: ${FormatUtils.formatCurrency(item.thermalPaperCost)}",
                                                    fontSize = 12.sp,
                                                    color = DarkSlate
                                                )
                                                Text(
                                                    text = "Labor: ${FormatUtils.formatCurrency(item.laborCost)}",
                                                    fontSize = 12.sp,
                                                    color = DarkSlate
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Price: ${FormatUtils.formatCurrency(item.sellingPrice)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DarkSlate
                                                )
                                                Text(
                                                    text = "Profit: ${FormatUtils.formatCurrency(item.profit)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (item.profit >= 0) Success else Color(
                                                        0xFFEF4444
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Order Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Order Summary",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            SummaryRow("Total Cost", FormatUtils.formatCurrency(currentOrder.totalCost))
                            SummaryRow("Total Price", FormatUtils.formatCurrency(currentOrder.totalPrice))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SummaryRow(
                                "Total Profit",
                                FormatUtils.formatCurrency(currentOrder.totalProfit),
                                isProfit = true,
                                profitValue = currentOrder.totalProfit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MediumGray,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = DarkSlate
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isProfit: Boolean = false,
    profitValue: Double = 0.0
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isProfit) 18.sp else 14.sp,
            fontWeight = if (isProfit) FontWeight.Bold else FontWeight.Medium,
            color = DarkSlate
        )
        Text(
            text = value,
            fontSize = if (isProfit) 18.sp else 14.sp,
            fontWeight = if (isProfit) FontWeight.Bold else FontWeight.Medium,
            color = if (isProfit && profitValue >= 0) Success else if (isProfit) AppColors.Error else Color(
                0xFF1E293B
            )
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = when (status) {
        "NEW" -> StatusBackground.New
        "IN_PROGRESS" -> StatusBackground.InProgress
        "READY" -> StatusBackground.Ready
        "COMPLETED" -> StatusBackground.Completed
        "CANCELLED" -> StatusBackground.Cancelled
        else -> VeryLightBluGray
    }

    val textColor = when (status) {
        "NEW" -> StatusText.New
        "IN_PROGRESS" -> StatusText.InProgress
        "READY" -> StatusText.Ready
        "COMPLETED" -> StatusText.Completed
        "CANCELLED" -> StatusText.Cancelled
        else -> DarkGrayText
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}