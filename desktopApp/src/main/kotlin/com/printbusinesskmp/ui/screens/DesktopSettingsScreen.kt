package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.platform.AppSettingsStore
import com.printbusinesskmp.desktop.platform.chooseDirectory
import com.printbusinesskmp.desktop.platform.openFile
import com.printbusinesskmp.navigation.Screen
import kotlinx.coroutines.launch

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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

        InvoiceNumberCard()
    }
}

@Composable
private fun InvoiceNumberCard() {
    val scope = rememberCoroutineScope()
    var nextNumber by remember { mutableStateOf<String?>(null) }
    var templateInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { ApiClient.getInvoiceNumberFormat() }
            .onSuccess {
                nextNumber = it.nextNumber
                templateInput = it.template
            }
            .onFailure { error = "Не вдалося завантажити формат: ${it.message}" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Нумерація рахунків-фактур",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Стала частина номера. Нулі в кінці визначають кількість цифр: " +
                    "СФ-0000000 → СФ-0000001, СФ-0000002… Номер призначається автоматично.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Наступний рахунок отримає номер: ${nextNumber ?: "…"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = templateInput,
                    onValueChange = { templateInput = it },
                    label = { Text("Стала частина") },
                    singleLine = true,
                    modifier = Modifier.width(220.dp)
                )
                Button(
                    onClick = {
                        val template = templateInput.trim()
                        if (template.isBlank() || !template.endsWith("0")) {
                            error = "Шаблон має закінчуватися нулями, наприклад СФ-0000000"
                            return@Button
                        }
                        saving = true
                        message = null
                        error = null
                        scope.launch {
                            runCatching { ApiClient.setInvoiceNumberFormat(template) }
                                .onSuccess {
                                    nextNumber = it.nextNumber
                                    templateInput = it.template
                                    message = "Збережено. Наступний номер: ${it.nextNumber}"
                                }
                                .onFailure { error = it.message ?: "Помилка збереження" }
                            saving = false
                        }
                    },
                    enabled = !saving
                ) {
                    Text("Зберегти")
                }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
