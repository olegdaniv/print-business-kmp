package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.navigation.Screen

@Composable
fun AppLayout(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Sidebar(
            currentScreen = currentScreen,
            onNavigate = onNavigate
        )

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Sidebar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(Color(0xFF1E293B))
            .padding(vertical = 24.dp)
    ) {
        // Logo/Title
        Text(
            text = "Print Business",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation items
        SidebarItem(
            text = "Dashboard",
            isSelected = currentScreen is Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) }
        )

        SidebarItem(
            text = "Clients",
            isSelected = currentScreen is Screen.Clients || currentScreen is Screen.ClientDetail,
            onClick = { onNavigate(Screen.Clients) }
        )

        SidebarItem(
            text = "Orders",
            isSelected = currentScreen is Screen.Orders || currentScreen is Screen.OrderDetail || currentScreen is Screen.NewOrder,
            onClick = { onNavigate(Screen.Orders) }
        )
    }
}

@Composable
private fun SidebarItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF3B82F6) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFFCBD5E1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}