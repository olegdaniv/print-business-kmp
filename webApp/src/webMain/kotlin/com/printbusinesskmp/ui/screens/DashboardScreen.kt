package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa

@Composable
fun DashboardScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
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

    Column {
        Text(
            text = "Огляд",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.DarkSlate,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard("Клієнти", clients.size.toString(), Modifier.weight(1f))
            DashboardCard("Замовлення", orders.size.toString(), Modifier.weight(1f))
            DashboardCard(
                "Прибуток",
                FormatUtils.formatCurrency(orders.sumOf { it.profit }),
                Modifier.weight(1f)
            )
            DashboardCard(
                "Активні",
                orders.count { it.status !in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED) }
                    .toString(),
                Modifier.weight(1f)
            )
        }

        Text(
            text = "Останні замовлення",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.DarkSlate,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val clientById = clients.associateBy { it.id }
        val recent = orders.sortedByDescending { it.updatedAt }.take(8)

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn {
                items(recent) { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "#${order.id.take(8)}",
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.DarkSlate
                            )
                            Text(
                                text = clientById[order.clientId]?.displayName ?: "Невідомий клієнт",
                                color = AppColors.MediumGray,
                                fontSize = 13.sp
                            )
                        }

                        Column {
                            Text(
                                text = order.status.labelUa(),
                                color = AppColors.DarkGrayText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = FormatUtils.formatCurrency(order.totalPrice),
                                color = AppColors.DarkSlate,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = FormatUtils.formatDate(order.updatedAt),
                                color = AppColors.MediumGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = AppColors.MediumGray, fontSize = 13.sp)
            Text(
                value,
                color = AppColors.DarkSlate,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}
