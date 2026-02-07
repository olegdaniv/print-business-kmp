package com.printbusinesskmp.ui.components

import androidx.compose.runtime.Composable
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.screens.BusinessProfileScreen
import com.printbusinesskmp.ui.screens.ClientFormScreen
import com.printbusinesskmp.ui.screens.ClientsScreen
import com.printbusinesskmp.ui.screens.DashboardScreen
import com.printbusinesskmp.ui.screens.InvoiceScreen
import com.printbusinesskmp.ui.screens.OrderDetailScreen
import com.printbusinesskmp.ui.screens.OrderFormScreen
import com.printbusinesskmp.ui.screens.OrdersScreen

@Composable
fun NavigationContent(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    when (currentScreen) {
        Screen.Dashboard -> DashboardScreen(onNavigate)
        Screen.BusinessProfile -> BusinessProfileScreen(onNavigate)
        Screen.Clients -> ClientsScreen(onNavigate)
        is Screen.ClientForm -> ClientFormScreen(currentScreen.clientId, onNavigate)
        Screen.Orders -> OrdersScreen(onNavigate)
        is Screen.OrderForm -> OrderFormScreen(currentScreen.orderId, onNavigate)
        is Screen.OrderDetail -> OrderDetailScreen(currentScreen.orderId, onNavigate)
        Screen.Invoices -> InvoiceScreen(onNavigate)
    }
}
