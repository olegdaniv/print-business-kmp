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
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(onNavigate: (Screen) -> Unit) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                orders = ApiClient.getOrders()
                clients = ApiClient.getClients()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load orders: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Orders",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Button(
                onClick = { onNavigate(Screen.NewOrder) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("+ New Order", color = Color.White)
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
        } else {
            // Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(16.dp)
                    ) {
                        TableHeaderCell("Order ID", Modifier.weight(1f))
                        TableHeaderCell("Client", Modifier.weight(1.5f))
                        TableHeaderCell("Status", Modifier.weight(1f))
                        TableHeaderCell("Items", Modifier.weight(0.8f))
                        TableHeaderCell("Total Price", Modifier.weight(1f))
                        TableHeaderCell("Profit", Modifier.weight(1f))
                        TableHeaderCell("Date", Modifier.weight(1f))
                        TableHeaderCell("Actions", Modifier.weight(1f))
                    }

                    HorizontalDivider()

                    // Table Rows
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(orders) { order ->
                            OrderRow(
                                order = order,
                                clients = clients,
                                onView = { onNavigate(Screen.OrderDetail(order.id)) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF475569),
        modifier = modifier
    )
}

@Composable
private fun OrderRow(
    order: Order,
    clients: List<Client>,
    onView: () -> Unit
) {
    val client = clients.find { it.id == order.clientId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${order.id.take(8)}",
            fontSize = 14.sp,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = client?.name ?: "Unknown",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(1.5f)
        )

        Box(modifier = Modifier.weight(1f)) {
            StatusBadge(status = order.status.name)
        }

        Text(
            text = order.items.size.toString(),
            fontSize = 14.sp,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(0.8f)
        )

        Text(
            text = FormatUtils.formatCurrency(order.totalPrice),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = FormatUtils.formatCurrency(order.totalProfit),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (order.totalProfit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = FormatUtils.formatDate(order.createdAt),
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(1f)
        )

        TextButton(
            onClick = onView,
            modifier = Modifier.weight(1f)
        ) {
            Text("View", color = Color(0xFF3B82F6))
        }
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