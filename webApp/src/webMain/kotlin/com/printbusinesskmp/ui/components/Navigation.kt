package com.printbusinesskmp.ui.components

import androidx.compose.runtime.Composable
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.screens.*

@Composable
fun NavigationContent(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    when (currentScreen) {
        is Screen.Dashboard -> DashboardScreen(onNavigate)
        is Screen.Clients -> ClientsScreen(onNavigate)
        is Screen.ClientDetail -> ClientFormScreen(
            clientId = currentScreen.clientId,
            onNavigate = onNavigate
        )
        is Screen.Orders -> OrdersScreen(onNavigate)
        is Screen.OrderDetail -> OrderDetailScreen(
            orderId = currentScreen.orderId,
            onNavigate = onNavigate
        )
        is Screen.OrderForm -> OrderFormScreen(
            orderId = currentScreen.orderId,
            onNavigate = onNavigate
        )
        is Screen.Invoices -> InvoiceScreen(onNavigate)
        is Screen.PricingCalculator -> PricingCalculatorScreen(onNavigate)
    }
}