package com.printbusinesskmp.ui.components

import androidx.compose.runtime.Composable
import com.printbusinesskmp.desktop.update.UpdateUiState
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.screens.BusinessProfileScreen
import com.printbusinesskmp.ui.screens.ClientFormScreen
import com.printbusinesskmp.ui.screens.ClientsScreen
import com.printbusinesskmp.ui.screens.DashboardScreen
import com.printbusinesskmp.ui.screens.InvoiceScreen
import com.printbusinesskmp.ui.screens.LayoutsScreen
import com.printbusinesskmp.ui.screens.OrderDetailScreen
import com.printbusinesskmp.ui.screens.OrderFormScreen
import com.printbusinesskmp.ui.screens.OrdersScreen
import com.printbusinesskmp.ui.screens.UpdatesScreen

@Composable
fun NavigationContent(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onCancelUpdateDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissUpdateError: () -> Unit
) {
    when (currentScreen) {
        Screen.Dashboard -> DashboardScreen(onNavigate)
        Screen.BusinessProfile -> BusinessProfileScreen(onNavigate)
        Screen.Clients -> ClientsScreen(onNavigate)
        is Screen.ClientForm -> ClientFormScreen(currentScreen.clientId, onNavigate)
        Screen.Orders -> OrdersScreen(onNavigate)
        is Screen.OrderForm -> OrderFormScreen(currentScreen.orderId, onNavigate)
        is Screen.OrderDetail -> OrderDetailScreen(currentScreen.orderId, onNavigate)
        Screen.Layouts -> LayoutsScreen(onNavigate)
        Screen.Invoices -> InvoiceScreen(onNavigate)
        Screen.Updates -> UpdatesScreen(
            state = updateState,
            onCheckForUpdates = onCheckForUpdates,
            onDownloadUpdate = onDownloadUpdate,
            onCancelDownload = onCancelUpdateDownload,
            onInstallUpdate = onInstallUpdate,
            onDismissError = onDismissUpdateError
        )
    }
}
