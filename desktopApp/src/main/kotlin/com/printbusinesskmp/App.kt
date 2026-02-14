package com.printbusinesskmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.printbusinesskmp.desktop.update.UpdateService
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.AppLayout
import com.printbusinesskmp.ui.components.NavigationContent

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Orders) }
    val updateService = remember { UpdateService() }
    val updateState by updateService.uiState.collectAsState()

    LaunchedEffect(Unit) {
        updateService.checkForUpdates()
    }

    MaterialTheme {
        AppLayout(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen },
            updateAvailable = updateState.updateAvailable
        ) {
            NavigationContent(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen },
                updateState = updateState,
                onCheckForUpdates = updateService::checkForUpdates,
                onDownloadUpdate = updateService::downloadLatestUpdate,
                onCancelUpdateDownload = updateService::cancelDownload,
                onInstallUpdate = updateService::installDownloadedUpdateAndExit,
                onDismissUpdateError = updateService::clearError
            )
        }
    }
}
