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
import com.printbusinesskmp.shared.resources.Res
import com.printbusinesskmp.shared.resources.app_name
import com.printbusinesskmp.shared.resources.nav_calculator
import com.printbusinesskmp.shared.resources.nav_clients
import com.printbusinesskmp.shared.resources.nav_dashboard
import com.printbusinesskmp.shared.resources.nav_orders
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.LightGray
import com.printbusinesskmp.theme.AppColors.LightGrayText
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.Transparent
import com.printbusinesskmp.theme.AppColors.White
import org.jetbrains.compose.resources.stringResource

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
                .background(LightGray)
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
            .background(DarkSlate)
            .padding(vertical = 24.dp)
    ) {
        // Logo/Title
        Text(
            text = stringResource(Res.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation items
        SidebarItem(
            text =
                stringResource(Res.string.nav_dashboard),
            isSelected = currentScreen is Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) }
        )

        SidebarItem(
            text = stringResource(Res.string.nav_clients),
            isSelected = currentScreen is Screen.Clients || currentScreen is Screen.ClientDetail,
            onClick = { onNavigate(Screen.Clients) }
        )

        SidebarItem(
            text = stringResource(Res.string.nav_orders),
            isSelected = currentScreen is Screen.Orders || currentScreen is Screen.OrderDetail || currentScreen is Screen.NewOrder,
            onClick = { onNavigate(Screen.Orders) }
        )

        SidebarItem(
            text = stringResource(Res.string.nav_calculator),
            isSelected = currentScreen is Screen.PricingCalculator,
            onClick = { onNavigate(Screen.PricingCalculator) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Language switcher
        LanguageSwitcher()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SidebarItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PrimaryBlue else Transparent
    val textColor = if (isSelected) White else LightGrayText

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