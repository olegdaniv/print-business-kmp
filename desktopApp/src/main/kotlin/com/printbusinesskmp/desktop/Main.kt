package com.printbusinesskmp.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.printbusinesskmp.App

fun main() {
    // Start the embedded local backend first, then point the API client at it
    // before any Compose code (and thus ApiClient) is initialized.
    val baseUrl = LocalServer.start()
    System.setProperty("printbusiness.api.baseUrl", baseUrl)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Souvenir Print",
            icon = painterResource("icon.png"),
            state = rememberWindowState(width = 1200.dp, height = 800.dp)
        ) {
            App()
        }
    }
}
