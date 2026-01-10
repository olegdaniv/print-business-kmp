package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.shared.resources.Res
import com.printbusinesskmp.shared.resources.nav_dashboard
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.MediumGray
import com.printbusinesskmp.theme.AppColors.StatusBackground.Cancelled
import com.printbusinesskmp.theme.AppColors.StatusBackground.Completed
import com.printbusinesskmp.theme.AppColors.StatusBackground.InProgress
import com.printbusinesskmp.theme.AppColors.StatusBackground.New
import com.printbusinesskmp.theme.AppColors.StatusBackground.Ready
import com.printbusinesskmp.theme.AppColors.StatusText
import com.printbusinesskmp.theme.AppColors.VeryLightBluGray
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(onNavigate: (Screen) -> Unit) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            println("DashboardScreen: Fetching data from API...")
            clients = ApiClient.getClients()
            println("DashboardScreen: Clients loaded - count: ${clients.size}")
            orders = ApiClient.getOrders()
            println("DashboardScreen: Orders loaded - count: ${orders.size}")
            isLoading = false
            println("DashboardScreen: Data loading complete")
        } catch (e: Exception) {
            println("DashboardScreen: Error - ${e.message}")
            e.printStackTrace()
            errorMessage = "Failed to load data: ${e.message}"
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.nav_dashboard),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                fontSize = 16.sp
            )
        } else {
            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Clients",
                    value = clients.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Total Orders",
                    value = orders.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Total Profit",
                    value = FormatUtils.formatCurrency(orders.sumOf { it.totalProfit }),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Active Orders",
                    value = orders.count { it.status.name != "COMPLETED" && it.status.name != "CANCELLED" }.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Orders
            Text(
                text = "Recent Orders",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkSlate,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(orders.take(5)) { order ->
                        RecentOrderItem(order = order, clients = clients)
                        if (order != orders.take(5).last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MediumGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )
        }
    }
}

@Composable
private fun RecentOrderItem(order: Order, clients: List<Client>) {
    val client = clients.find { it.id == order.clientId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Order #${order.id.take(8)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkSlate
            )
            Text(
                text = client?.name ?: "Unknown Client",
                fontSize = 14.sp,
                color = MediumGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            StatusBadge(status = order.status.name)
            Text(
                text = FormatUtils.formatCurrency(order.totalPrice),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkSlate,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = when (status) {
        "NEW" -> New
        "IN_PROGRESS" -> InProgress
        "READY" -> Ready
        "COMPLETED" -> Completed
        "CANCELLED" -> Cancelled
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