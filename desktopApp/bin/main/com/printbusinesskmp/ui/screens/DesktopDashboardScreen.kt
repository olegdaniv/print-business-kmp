package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.StatCard
import com.printbusinesskmp.ui.components.StatusBadge
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils

@Composable
fun DesktopDashboardScreen(onNavigate: (Screen) -> Unit) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            clients = ApiClient.getClients()
            orders = ApiClient.getOrders()
        } catch (e: Exception) {
            error = e.message ?: "Помилка завантаження"
        } finally {
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    val activeOrders = orders.count { !it.status.isFinal }
    val totalRevenue = orders.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.totalPrice }
    val totalProfit = orders.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.profit }
    val pendingPayment = orders.count {
        it.paymentStatus != com.printbusinesskmp.models.PaymentStatus.PAID && !it.status.isFinal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Огляд",
                style = MaterialTheme.typography.displaySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigate(Screen.OrderForm(null)) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Text(" Нове замовлення")
                }
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Активні замовлення",
                value = activeOrders.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Клієнти",
                value = clients.size.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Виручка",
                value = FormatUtils.formatCurrency(totalRevenue),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Прибуток",
                value = FormatUtils.formatCurrency(totalProfit),
                valueColor = if (totalProfit >= 0) DesktopColors.success else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Two-column layout: recent orders + quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent orders
            Card(
                modifier = Modifier.weight(2f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Останні замовлення",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { onNavigate(Screen.Orders) }) {
                            Text("Дивитись всі")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val clientById = clients.associateBy { it.id }
                    val recent = orders.sortedByDescending { it.updatedAt }.take(8)

                    recent.forEachIndexed { index, order ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        HoverableRow(
                            onClick = { onNavigate(Screen.Orders) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "#${order.id.take(8)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        clientById[order.clientId]?.displayName ?: "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(order.status)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        FormatUtils.formatCurrency(order.totalPrice),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status breakdown
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "По статусах",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    @Suppress("DEPRECATION")
                    val statusCounts = orders
                        .groupBy { it.status }
                        .toSortedMap(compareBy { it.ordinal })

                    statusCounts.forEach { (status, statusOrders) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(status)
                            Text(
                                statusOrders.size.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Очікують оплати",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        pendingPayment.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (pendingPayment > 0) DesktopColors.warning else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
