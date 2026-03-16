package com.printbusinesskmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.api.AuthRequestException
import com.printbusinesskmp.api.AuthSession
import com.printbusinesskmp.api.NotAllowlistedException
import com.printbusinesskmp.auth.DesktopGoogleSignInService
import com.printbusinesskmp.desktop.update.UpdateService
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.AppShell
import com.printbusinesskmp.ui.components.NavigationContent
import com.printbusinesskmp.ui.screens.LoginScreen
import com.printbusinesskmp.ui.theme.PrintBusinessDesktopTheme
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Orders) }
    var authSession by remember { mutableStateOf<AuthSession?>(null) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    val updateService = remember { UpdateService() }
    val googleSignInService = remember { DesktopGoogleSignInService() }
    val updateState by updateService.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ApiClient.setUnauthorizedHandler {
            ApiClient.setAccessToken(null)
            authSession = null
            authMessage = "Your session expired. Please sign in again."
        }
    }

    PrintBusinessDesktopTheme(darkTheme = isDarkTheme) {
        if (authSession == null) {
            LoginScreen(
                title = "Souvenir Print",
                message = authMessage,
                isLoading = isSigningIn,
                onSignInClick = {
                    coroutineScope.launch {
                        isSigningIn = true
                        authMessage = null
                        try {
                            val googleIdToken = googleSignInService.requestIdToken()
                            val signedInSession = ApiClient.exchangeGoogleIdToken(googleIdToken)
                            ApiClient.setAccessToken(signedInSession.accessToken)
                            authSession = signedInSession
                            currentScreen = Screen.Orders
                            updateService.checkForUpdates()
                        } catch (error: NotAllowlistedException) {
                            authMessage = "not authorized $error"
                        } catch (error: AuthRequestException) {
                            authMessage = error.message ?: "Sign-in failed. Please try again."
                        } catch (error: Exception) {
                            authMessage = error.message ?: "Sign-in failed. Please try again."
                        } finally {
                            isSigningIn = false
                        }
                    }
                }
            )
            return@PrintBusinessDesktopTheme
        }

        AppShell(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen },
            isDarkTheme = isDarkTheme,
            onToggleTheme = { isDarkTheme = !isDarkTheme },
            updateAvailable = updateState.updateAvailable,
            signedInLabel = signedInLabel(authSession)
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

private fun signedInLabel(session: AuthSession?): String? {
    session ?: return null
    val displayName = session.name?.trim()?.takeIf { it.isNotEmpty() } ?: session.email
    return "Signed in as $displayName"
}
