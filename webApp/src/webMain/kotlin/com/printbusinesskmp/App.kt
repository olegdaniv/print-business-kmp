package com.printbusinesskmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.api.AuthRequestException
import com.printbusinesskmp.api.AuthSession
import com.printbusinesskmp.api.NotAllowlistedException
import com.printbusinesskmp.auth.GoogleIdentityService
import com.printbusinesskmp.auth.PersistedAuthSession
import com.printbusinesskmp.auth.WebAuthStorage
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.AppLayout
import com.printbusinesskmp.ui.components.NavigationContent
import com.printbusinesskmp.ui.screens.LoginScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var persistedSession by remember { mutableStateOf(WebAuthStorage.load()) }
    var loginMessage by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val googleClientId = remember { GoogleIdentityService.readClientId() }

    LaunchedEffect(Unit) {
        ApiClient.setUnauthorizedHandler {
            ApiClient.setAccessToken(null)
            WebAuthStorage.clear()
            persistedSession = null
            isSigningIn = false
            loginMessage = "Your session expired. Please sign in again."
        }
        ApiClient.setAccessToken(persistedSession?.accessToken)
    }

    LaunchedEffect(googleClientId) {
        if (persistedSession != null) return@LaunchedEffect
        val clientId = googleClientId
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.takeUnless { isPlaceholderGoogleClientId(it) }
        if (clientId.isNullOrBlank()) {
            loginMessage =
                "Google client ID is missing/placeholder. Set a real Web OAuth client ID in GOOGLE_CLIENT_ID (.env) " +
                    "or -Pprintbusiness.google.clientId, " +
                    "or set window.__PRINTBUSINESS_GOOGLE_CLIENT_ID in index.html."
            return@LaunchedEffect
        }

        val onCredentialReceived: (String) -> Unit = { idToken ->
            coroutineScope.launch {
                isSigningIn = true
                loginMessage = null
                try {
                    val authSession = ApiClient.exchangeGoogleIdToken(idToken)
                    applySession(authSession) { session ->
                        persistedSession = session
                    }
                } catch (error: NotAllowlistedException) {
                    loginMessage = "This Google account is not authorized for this workspace."
                } catch (error: AuthRequestException) {
                    loginMessage = error.message ?: "Sign-in failed. Please try again."
                } catch (error: Exception) {
                    loginMessage = error.message ?: "Sign-in failed. Please try again."
                } finally {
                    isSigningIn = false
                }
            }
        }

        var initialized = false
        var attempt = 0
        while (!initialized && attempt < 20) {
            initialized = GoogleIdentityService.initialize(clientId, onCredentialReceived)
            if (!initialized) {
                delay(250)
            }
            attempt += 1
        }

        if (!initialized) {
            loginMessage = "Google Sign-In is unavailable. Check that the GIS script is loaded, then refresh."
        }
    }

    if (persistedSession == null) {
        MaterialTheme {
            LoginScreen(
                title = "Print Business",
                message = loginMessage,
                isLoading = isSigningIn,
                onSignInClick = {
                    val prompted = GoogleIdentityService.promptSignIn()
                    if (!prompted) {
                        loginMessage = "Google Sign-In is still initializing. Refresh the page and try again."
                    }
                }
            )
        }
        return
    }

    MaterialTheme {
        AppLayout(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen },
            signedInLabel = signedInLabel(persistedSession)
        ) {
            NavigationContent(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen }
            )
        }
    }
}

private fun applySession(
    session: AuthSession,
    onStored: (PersistedAuthSession?) -> Unit
) {
    ApiClient.setAccessToken(session.accessToken)
    WebAuthStorage.save(session)
    onStored(WebAuthStorage.load())
}

private fun signedInLabel(session: PersistedAuthSession?): String? {
    session ?: return null
    val displayName = session.name?.trim()?.takeIf { it.isNotEmpty() } ?: session.email
    return "Signed in as $displayName"
}

private fun isPlaceholderGoogleClientId(clientId: String): Boolean {
    val normalized = clientId.trim().lowercase()
    return normalized.contains("dummy-google-client-id") ||
        normalized.contains("your-google-client-id") ||
        normalized == "changeme"
}
