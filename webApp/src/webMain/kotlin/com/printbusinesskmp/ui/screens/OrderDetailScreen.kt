package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                color = Color(0xFF1E293B)
            )

            TextButton(onClick = { onNavigate(Screen.Orders) }) {
                Text("← Back to Orders", color = Color(0xFF3B82F6))
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
        } else if (order != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Order Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Order Information",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            InfoRow("Order ID", "#${order!!.id}")
                            InfoRow("Client", client?.name ?: "Unknown")
                            InfoRow("Status", "")
                            Box(modifier = Modifier.padding(start = 120.dp, top = 4.dp)) {
                                StatusBadge(status = order!!.status.name)
                            }
                            InfoRow("Created", FormatUtils.formatDateTime(order!!.createdAt))
                            InfoRow("Updated", FormatUtils.formatDateTime(order!!.updatedAt))
                            order!!.completedAt?.let {
                                InfoRow("Completed", FormatUtils.formatDateTime(it))
                            }

                            if (order!!.notes != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Notes:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = order!!.notes ?: "",
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Status Update
                            if (order!!.status != OrderStatus.COMPLETED && order!!.status != OrderStatus.CANCELLED) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Update Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
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
                                                containerColor = if (status == order!!.status) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Text(
                                                text = status.name.replace("_", " "),
                                                color = if (status == order!!.status) Color.White else Color(0xFF64748B),
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Order Items (${order!!.items.size})",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            order!!.items.forEach { item ->
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
                                            Text(
                                                text = "Qty: ${item.quantity}",
                                                fontSize = 14.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (item.size != null || item.color != null) {
                                            Text(
                                                text = listOfNotNull(item.size, item.color).joinToString(", "),
                                                fontSize = 14.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Text(
                                            text = "Print Area: ${item.printArea.name}",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B)
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
                                                    color = Color(0xFF64748B)
                                                )
                                                Text(
                                                    text = "Blank: ${FormatUtils.formatCurrency(item.blankItemCost)}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Text(
                                                    text = "Paper: ${FormatUtils.formatCurrency(item.thermalPaperCost)}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Text(
                                                    text = "Labor: ${FormatUtils.formatCurrency(item.laborCost)}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Price: ${FormatUtils.formatCurrency(item.sellingPrice)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E293B)
                                                )
                                                Text(
                                                    text = "Profit: ${FormatUtils.formatCurrency(item.profit)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (item.profit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444)
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Order Summary",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            SummaryRow("Total Cost", FormatUtils.formatCurrency(order!!.totalCost))
                            SummaryRow("Total Price", FormatUtils.formatCurrency(order!!.totalPrice))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SummaryRow(
                                "Total Profit",
                                FormatUtils.formatCurrency(order!!.totalProfit),
                                isProfit = true,
                                profitValue = order!!.totalProfit
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
            color = Color(0xFF64748B),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF1E293B)
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
            color = Color(0xFF1E293B)
        )
        Text(
            text = value,
            fontSize = if (isProfit) 18.sp else 14.sp,
            fontWeight = if (isProfit) FontWeight.Bold else FontWeight.Medium,
            color = if (isProfit && profitValue >= 0) Color(0xFF16A34A) else if (isProfit) Color(0xFFEF4444) else Color(0xFF1E293B)
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = when (status) {
        "NEW" -> Color(0xFFDCFCE7)
        "IN_PROGRESS" -> Color(0xFFDEEDFF)
        "READY" -> Color(0xFFFEF3C7)
        "COMPLETED" -> Color(0xFFD1FAE5)
        "CANCELLED" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }

    val textColor = when (status) {
        "NEW" -> Color(0xFF166534)
        "IN_PROGRESS" -> Color(0xFF1E40AF)
        "READY" -> Color(0xFF854D0E)
        "COMPLETED" -> Color(0xFF065F46)
        "CANCELLED" -> Color(0xFF991B1B)
        else -> Color(0xFF475569)
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