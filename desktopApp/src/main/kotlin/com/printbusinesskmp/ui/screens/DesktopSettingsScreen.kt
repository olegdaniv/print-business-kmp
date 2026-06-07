package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.desktop.platform.AppSettingsStore
import com.printbusinesskmp.desktop.platform.chooseDirectory
import com.printbusinesskmp.desktop.platform.openFile
import com.printbusinesskmp.navigation.Screen

@Composable
fun DesktopSettingsScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    var invoicesDir by remember { mutableStateOf(AppSettingsStore.invoicesDir.toString()) }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Налаштування",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Папка для інвойсів",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = invoicesDir,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Сюди зберігаються PDF-інвойси при генерації; звідси вони відкриваються.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val chosen = chooseDirectory("Оберіть папку для інвойсів")
                        if (chosen != null) {
                            AppSettingsStore.invoicesDir = chosen
                            invoicesDir = AppSettingsStore.invoicesDir.toString()
                        }
                    }) {
                        Text("Змінити папку")
                    }
                    OutlinedButton(onClick = {
                        runCatching { openFile(AppSettingsStore.invoicesDir) }
                    }) {
                        Text("Відкрити папку")
                    }
                }
            }
        }
    }
}
