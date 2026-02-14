package com.printbusinesskmp.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.printbusinesskmp.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Print Business Pro",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
