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
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.CardItemBg
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.MediumGray
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.StatusBackground
import com.printbusinesskmp.theme.AppColors.Success
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.shared.resources.*
import com.printbusinesskmp.theme.AppColors.StatusText
import com.printbusinesskmp.theme.AppColors.VeryLightBluGray
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
                errorMessage = "${getString(Res.string.error_load_orders)}: ${e.message}"
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
                text = stringResource(Res.string.orders_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )

            Button(
                onClick = { onNavigate(Screen.OrderForm()) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(stringResource(Res.string.orders_new_button), color = White)
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
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardItemBg)
                            .padding(16.dp)
                    ) {
                        TableHeaderCell(
                            stringResource(Res.string.table_header_order_id),
                            Modifier.weight(1f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_client),
                            Modifier.weight(1.5f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_status),
                            Modifier.weight(1f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_items),
                            Modifier.weight(0.8f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_total_price),
                            Modifier.weight(1f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_profit),
                            Modifier.weight(1f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_date),
                            Modifier.weight(1f)
                        )
                        TableHeaderCell(
                            stringResource(Res.string.table_header_actions),
                            Modifier.weight(1f)
                        )
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
        color = DarkGrayText,
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
            color = DarkSlate,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = client?.name ?: stringResource(Res.string.orders_unknown_client),
            fontSize = 14.sp,
            color = MediumGray,
            modifier = Modifier.weight(1.5f)
        )

        Box(modifier = Modifier.weight(1f)) {
            StatusBadge(status = order.status.name)
        }

        Text(
            text = order.items.size.toString(),
            fontSize = 14.sp,
            color = DarkSlate,
            modifier = Modifier.weight(0.8f)
        )

        Text(
            text = FormatUtils.formatCurrency(
                order.totalPrice,
                stringResource(Res.string.format_currency_suffix)
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkSlate,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = FormatUtils.formatCurrency(
                order.totalProfit,
                stringResource(Res.string.format_currency_suffix)
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (order.totalProfit >= 0) Success else AppColors.Error,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = FormatUtils.formatDate(order.createdAt),
            fontSize = 14.sp,
            color = MediumGray,
            modifier = Modifier.weight(1f)
        )

        TextButton(
            onClick = onView,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(Res.string.action_view), color = PrimaryBlue)
        }
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