package com.printbusinesskmp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.printbusinesskmp.desktop.update.UpdateUiState
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.screens.BusinessProfileScreen
import com.printbusinesskmp.ui.screens.ClientFormScreen
import com.printbusinesskmp.ui.screens.DesktopClientsScreen
import com.printbusinesskmp.ui.screens.DesktopDashboardScreen
import com.printbusinesskmp.ui.screens.InvoiceScreen
import com.printbusinesskmp.ui.screens.DesktopLayoutsPlaceholder
import com.printbusinesskmp.ui.screens.DesktopOrdersScreen
import com.printbusinesskmp.ui.screens.DesktopPaymentsScreen
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
    // Keyed by screen so per-screen state never carries over between destinations of
    // the same type (e.g. Ctrl+N while editing an order must open an empty form).
    key(currentScreen) {
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
            is Screen.ClientForm -> FormContainer { ClientFormScreen(currentScreen.clientId, onNavigate) }
            Screen.Orders -> DesktopOrdersScreen(onNavigate)
            is Screen.OrderForm -> FormContainer { OrderFormScreen(currentScreen.orderId, onNavigate) }
            is Screen.OrderDetail -> {
                // Detail is inline in the split pane — open Orders with this order selected
                DesktopOrdersScreen(onNavigate, initialOrderId = currentScreen.orderId)
            }
            Screen.Layouts -> DesktopLayoutsPlaceholder(onNavigate)
            Screen.Invoices -> InvoiceScreen(onNavigate)
            Screen.Payments -> DesktopPaymentsScreen(onNavigate)
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
}

/** Shared form screens bring no outer padding of their own (the web shell adds it). */
@Composable
private fun FormContainer(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        content()
    }
}
