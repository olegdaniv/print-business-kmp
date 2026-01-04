package com.printbusinesskmp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    window.onload = {
        val loading = document.getElementById("loading")
        loading?.remove()

        ComposeViewport("root") {
            App()
        }
    }
}