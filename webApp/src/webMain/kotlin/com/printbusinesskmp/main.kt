package com.printbusinesskmp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    window.onload = {
        val root = document.getElementById("root") ?: error("Root element not found")
        val loading = document.getElementById("loading")
        loading?.remove()

        ComposeViewport(root) {
            App()
        }
    }
}