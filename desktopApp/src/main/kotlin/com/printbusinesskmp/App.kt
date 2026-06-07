package com.printbusinesskmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.printbusinesskmp.desktop.update.UpdateService
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.AppShell
import com.printbusinesskmp.ui.components.NavigationContent
import com.printbusinesskmp.ui.theme.PrintBusinessDesktopTheme

@Composable
fun App() {
    // Local-first desktop: data lives in a local H2 database served by the
    // embedded in-process backend. No sign-in — the app opens straight in.
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Orders) }
    var isDarkTheme by remember { mutableStateOf(false) }
    val updateService = remember { UpdateService() }
    val updateState by updateService.uiState.collectAsState()

    LaunchedEffect(Unit) {
        updateService.checkForUpdates()
    }

    PrintBusinessDesktopTheme(darkTheme = isDarkTheme) {
        AppShell(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen },
            isDarkTheme = isDarkTheme,
            onToggleTheme = { isDarkTheme = !isDarkTheme },
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
