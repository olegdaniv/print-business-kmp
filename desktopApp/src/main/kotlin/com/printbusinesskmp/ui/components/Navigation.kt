package com.printbusinesskmp.ui.components

import androidx.compose.runtime.Composable
import com.printbusinesskmp.desktop.update.UpdateUiState
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.screens.BusinessProfileScreen
import com.printbusinesskmp.ui.screens.ClientFormScreen
import com.printbusinesskmp.ui.screens.DesktopClientsScreen
import com.printbusinesskmp.ui.screens.DesktopDashboardScreen
import com.printbusinesskmp.ui.screens.InvoiceScreen
import com.printbusinesskmp.ui.screens.DesktopLayoutsPlaceholder
import com.printbusinesskmp.ui.screens.DesktopOrdersScreen
import com.printbusinesskmp.ui.screens.DesktopSettingsScreen
import com.printbusinesskmp.ui.screens.OrderFormScreen
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
        Screen.Dashboard -> DesktopDashboardScreen(
            onNavigate = onNavigate,
            updateState = updateState,
            onDownloadUpdate = onDownloadUpdate,
            onCancelUpdateDownload = onCancelUpdateDownload,
            onInstallUpdate = onInstallUpdate
        )
        Screen.BusinessProfile -> BusinessProfileScreen(onNavigate)
        Screen.Clients -> DesktopClientsScreen(onNavigate)
        is Screen.ClientForm -> ClientFormScreen(currentScreen.clientId, onNavigate)
        Screen.Orders -> DesktopOrdersScreen(onNavigate)
        is Screen.OrderForm -> OrderFormScreen(currentScreen.orderId, onNavigate)
        is Screen.OrderDetail -> {
            // Redirect to Orders screen — detail is now inline in the split pane
            DesktopOrdersScreen(onNavigate)
        }
        Screen.Layouts -> DesktopLayoutsPlaceholder(onNavigate)
        Screen.Invoices -> InvoiceScreen(onNavigate)
        Screen.Updates -> UpdatesScreen(
            state = updateState,
            onCheckForUpdates = onCheckForUpdates,
            onDownloadUpdate = onDownloadUpdate,
            onCancelDownload = onCancelUpdateDownload,
            onInstallUpdate = onInstallUpdate,
            onDismissError = onDismissUpdateError
        )
        Screen.Settings -> DesktopSettingsScreen(onNavigate)
    }
}
