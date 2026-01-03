package com.printbusinesskmp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.AppLayout
import com.printbusinesskmp.ui.components.NavigationContent

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    MaterialTheme {
        print(" Start")
        AppLayout(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen }
        ) {

            NavigationContent(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen }
            )
        }
    }
}