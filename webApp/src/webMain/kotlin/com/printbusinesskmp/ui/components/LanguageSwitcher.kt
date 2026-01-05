package com.printbusinesskmp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.printbusinesskmp.localization.Language
import com.printbusinesskmp.localization.LocalizationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSwitcher(modifier: Modifier = Modifier) {
    val currentLang by LocalizationState.currentLanguage
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = true
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF3B82F6)
            )
        ) {
            Text(currentLang.displayName)
            Spacer(Modifier.width(4.dp))
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        LocalizationState.setLanguage(language)
                        expanded = false
                    },
                    leadingIcon = if (language == currentLang) {
                        { Text("✓", color = Color(0xFF16A34A)) }
                    } else null
                )
            }
        }
    }
}