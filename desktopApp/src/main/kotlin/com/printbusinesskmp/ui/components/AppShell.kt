package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.theme.DesktopColors

data class NavDestination(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
    val matchScreens: (Screen) -> Boolean
)

private val mainDestinations = listOf(
    NavDestination(
        label = "Огляд",
        icon = Icons.Default.Dashboard,
        screen = Screen.Dashboard,
        matchScreens = { it is Screen.Dashboard }),
    NavDestination(
        label = "Замовлення",
        icon = Icons.AutoMirrored.Filled.List,
        screen = Screen.Orders,
        matchScreens = { it is Screen.Orders || it is Screen.OrderDetail || it is Screen.OrderForm }),
    NavDestination(
        label = "Клієнти",
        icon = Icons.Default.People,
        screen = Screen.Clients,
        matchScreens = { it is Screen.Clients || it is Screen.ClientForm }),
    // Тимчасово приховано — рахунки генеруються з картки замовлення
    // NavDestination(
    //     label = "Рахунки",
    //     icon = Icons.Default.Receipt,
    //     screen = Screen.Invoices,
    //     matchScreens = { it is Screen.Invoices }),
    NavDestination(
        label = "Профіль",
        icon = Icons.Default.Business,
        screen = Screen.BusinessProfile,
        matchScreens = { it is Screen.BusinessProfile }),
    NavDestination(
        label = "Налаштування",
        icon = Icons.Default.Settings,
        screen = Screen.Settings,
        matchScreens = { it is Screen.Settings }),
)

@Composable
fun AppShell(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    updateAvailable: Boolean = false,
    signedInLabel: String? = null,
    onSignOut: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    // Digits follow the visible rail order so the Ctrl+N hints stay truthful
                    val digitIndex = when (event.key) {
                        Key.One -> 0
                        Key.Two -> 1
                        Key.Three -> 2
                        Key.Four -> 3
                        Key.Five -> 4
                        else -> null
                    }
                    when {
                        digitIndex != null && digitIndex < mainDestinations.size -> {
                            onNavigate(mainDestinations[digitIndex].screen); true
                        }

                        event.key == Key.N -> {
                            onNavigate(Screen.OrderForm(null)); true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }) {
        NavigationRail(
            currentScreen = currentScreen,
            onNavigate = onNavigate,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            updateAvailable = updateAvailable,
            signedInLabel = signedInLabel,
            onSignOut = onSignOut
        )

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

@Composable
private fun NavigationRail(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    updateAvailable: Boolean,
    signedInLabel: String?,
    onSignOut: (() -> Unit)?
) {
    val railBg = if (isDarkTheme) DesktopColors.railBackgroundDark else DesktopColors.railBackground

    Surface(
        modifier = Modifier.width(72.dp).fillMaxHeight(), color = railBg, tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo / brand mark
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(DesktopColors.railSelected), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Main nav items
            mainDestinations.forEachIndexed { index, dest ->
                RailItem(
                    icon = dest.icon,
                    label = dest.label,
                    selected = dest.matchScreens(currentScreen),
                    onClick = { onNavigate(dest.screen) },
                    shortcutHint = "Ctrl+${index + 1}"
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.weight(1f))

            // Update indicator
            if (updateAvailable || currentScreen is Screen.Updates) {
                RailItem(
                    icon = Icons.Default.SystemUpdate,
                    label = "Оновлення",
                    selected = currentScreen is Screen.Updates,
                    onClick = { onNavigate(Screen.Updates) },
                    badge = updateAvailable
                )
                Spacer(Modifier.height(8.dp))
            }

            // Theme toggle
            IconButton(
                onClick = onToggleTheme, modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = DesktopColors.railUnselected,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onSignOut != null) {
                IconButton(
                    onClick = onSignOut, modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign out",
                        tint = DesktopColors.railUnselected,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    shortcutHint: String? = null,
    badge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        selected -> DesktopColors.railSelected.copy(alpha = 0.15f)
        isHovered -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val iconColor = when {
        selected -> DesktopColors.railSelected
        isHovered -> Color.White
        else -> DesktopColors.railUnselected
    }

    val textColor = when {
        selected -> Color.White
        isHovered -> Color.White.copy(alpha = 0.9f)
        else -> DesktopColors.railUnselected
    }

    Box(
        modifier = Modifier.width(64.dp).clip(RoundedCornerShape(12.dp))
            .hoverable(interactionSource).clickable(
                interactionSource = interactionSource, indication = null, onClick = onClick
            ).background(bgColor).padding(vertical = 6.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
                if (badge) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape)
                            .background(DesktopColors.success)
                    )
                }
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
