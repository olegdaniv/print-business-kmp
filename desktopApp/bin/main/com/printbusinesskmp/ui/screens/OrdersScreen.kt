package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                clients = ApiClient.getClients()
                orders = ApiClient.getOrders()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Замовлення",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkSlate
            )

            Button(
                onClick = { onNavigate(Screen.OrderForm(null)) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
            ) {
                Text("+ Нове замовлення", color = AppColors.White)
            }
        }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        val clientById = clients.associateBy { it.id }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardItemBg)
                    .padding(14.dp)
            ) {
                Header("ID", Modifier.weight(1f))
                Header("Клієнт", Modifier.weight(1.8f))
                Header("Статус", Modifier.weight(1f))
                Header("Оплата", Modifier.weight(1f))
                Header("Позиції", Modifier.weight(0.8f))
                Header("Сума", Modifier.weight(1f))
                Header("Прибуток", Modifier.weight(1f))
                Header("Оновлено", Modifier.weight(1f))
                Header("", Modifier.weight(0.8f))
            }
            HorizontalDivider()

            LazyColumn {
                items(orders.sortedByDescending { it.updatedAt }) { order ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${order.id.take(8)}", Modifier.weight(1f), color = AppColors.DarkSlate)
                        Text(
                            clientById[order.clientId]?.displayName ?: "Невідомий",
                            Modifier.weight(1.8f),
                            color = AppColors.MediumGray,
                            fontSize = 13.sp
                        )
                        Text(order.status.labelUa(), Modifier.weight(1f), color = AppColors.DarkGrayText, fontSize = 12.sp)
                        Text(order.paymentStatus.labelUa(), Modifier.weight(1f), color = AppColors.DarkGrayText, fontSize = 12.sp)
                        Text(order.items.size.toString(), Modifier.weight(0.8f), color = AppColors.DarkSlate)
                        Text(FormatUtils.formatCurrency(order.totalPrice), Modifier.weight(1f), color = AppColors.DarkSlate)
                        Text(
                            FormatUtils.formatCurrency(order.profit),
                            Modifier.weight(1f),
                            color = if (order.profit >= 0) AppColors.Success else AppColors.Error,
                            fontSize = 13.sp
                        )
                        Text(
                            FormatUtils.formatDate(order.updatedAt),
                            Modifier.weight(1f),
                            color = AppColors.MediumGray,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { onNavigate(Screen.OrderDetail(order.id)) }, modifier = Modifier.weight(0.8f)) {
                            Text("Відкрити", color = AppColors.PrimaryBlue, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, fontSize = 13.sp)
}
