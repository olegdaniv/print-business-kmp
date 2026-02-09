package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors

@Composable
fun AppLayout(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(currentScreen = currentScreen, onNavigate = onNavigate)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.LightGray)
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
            .width(260.dp)
            .fillMaxHeight()
            .background(AppColors.DarkSlate)
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Print Business",
            color = AppColors.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        SidebarItem(
            text = "Огляд",
            selected = currentScreen is Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) }
        )
        SidebarItem(
            text = "Профіль ФОП",
            selected = currentScreen is Screen.BusinessProfile,
            onClick = { onNavigate(Screen.BusinessProfile) }
        )
        SidebarItem(
            text = "Клієнти",
            selected = currentScreen is Screen.Clients || currentScreen is Screen.ClientForm,
            onClick = { onNavigate(Screen.Clients) }
        )
        SidebarItem(
            text = "Замовлення",
            selected = currentScreen is Screen.Orders || currentScreen is Screen.OrderDetail || currentScreen is Screen.OrderForm,
            onClick = { onNavigate(Screen.Orders) }
        )
        SidebarItem(
            text = "Макети",
            selected = currentScreen is Screen.Layouts,
            onClick = { onNavigate(Screen.Layouts) }
        )
        SidebarItem(
            text = "Рахунки",
            selected = currentScreen is Screen.Invoices,
            onClick = { onNavigate(Screen.Invoices) }
        )
    }
}

@Composable
private fun SidebarItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) AppColors.PrimaryBlue else AppColors.Transparent
    val color = if (selected) AppColors.White else AppColors.LightGrayText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
