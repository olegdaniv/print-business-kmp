package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.localization.Language
import com.printbusinesskmp.localization.LanguageManager

@Composable
fun LanguageSwitcher() {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage = LanguageManager.currentLanguage.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Current language display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF334155), shape = MaterialTheme.shapes.small)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentLanguage.displayName,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = Color.White,
                fontSize = 12.sp
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        LanguageManager.setLanguage(language)
                        expanded = false
                    }
                )
            }
        }
    }
}
