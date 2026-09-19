package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.platform.AppSettingsStore
import com.printbusinesskmp.desktop.platform.chooseDirectory
import com.printbusinesskmp.desktop.platform.openFile
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.ScreenHeader
import com.printbusinesskmp.ui.components.SectionCard
import com.printbusinesskmp.ui.theme.DesktopColors
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
fun DesktopSettingsScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    var invoicesDir by remember { mutableStateOf(AppSettingsStore.invoicesDir.toString()) }
    var deliveryNotesDir by remember { mutableStateOf(AppSettingsStore.deliveryNotesDir.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Налаштування",
            subtitle = "Куди зберігати документи і як їх нумерувати"
        )

        Column(
            modifier = Modifier.widthIn(max = 1100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FolderSetting(
                    title = "Рахунки-фактури",
                    description = "Сюди зберігаються PDF рахунків при генерації; звідси вони відкриваються.",
                    path = invoicesDir,
                    onChange = {
                        chooseDirectory("Оберіть папку для рахунків", AppSettingsStore.invoicesDir)?.let { chosen ->
                            AppSettingsStore.invoicesDir = chosen
                            invoicesDir = AppSettingsStore.invoicesDir.toString()
                        }
                    },
                    onOpen = { AppSettingsStore.invoicesDir },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                FolderSetting(
                    title = "Видаткові накладні",
                    description = "PDF видаткових накладних (ВН) — окремо від рахунків.",
                    path = deliveryNotesDir,
                    onChange = {
                        chooseDirectory("Оберіть папку для видаткових накладних", AppSettingsStore.deliveryNotesDir)
                            ?.let { chosen ->
                                AppSettingsStore.deliveryNotesDir = chosen
                                deliveryNotesDir = AppSettingsStore.deliveryNotesDir.toString()
                            }
                    },
                    onOpen = { AppSettingsStore.deliveryNotesDir },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            InvoiceNumberCard()
        }
    }
}

@Composable
private fun FolderSetting(
    title: String,
    description: String,
    path: String,
    onChange: () -> Unit,
    onOpen: () -> Path,
    modifier: Modifier = Modifier
) {
    SectionCard(title = title, subtitle = description, modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    path,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onChange, shape = RoundedCornerShape(8.dp)) { Text("Змінити") }
            OutlinedButton(
                onClick = { runCatching { openFile(onOpen()) } },
                shape = RoundedCornerShape(8.dp)
            ) { Text("Відкрити") }
        }
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

    SectionCard(
        title = "Нумерація рахунків-фактур",
        subtitle = "Стала частина номера. Нулі в кінці визначають кількість цифр: " +
            "СФ-0000000 → СФ-0000001, СФ-0000002… Номер призначається автоматично."
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = templateInput,
                onValueChange = { templateInput = it },
                label = { Text("Шаблон") },
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
                                message = "Збережено"
                            }
                            .onFailure { error = it.message ?: "Помилка збереження" }
                        saving = false
                    }
                },
                enabled = !saving,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Зберегти")
            }
            Column {
                Text(
                    "Наступний номер",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    nextNumber ?: "…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = DesktopColors.success)
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
