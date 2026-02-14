@file:OptIn(ExperimentalMaterial3Api::class)

package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.platform.chooseImageFilePath
import com.printbusinesskmp.desktop.platform.chooseSavePath
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Layout
import com.printbusinesskmp.models.LayoutCreateRequest
import com.printbusinesskmp.models.LayoutStatus
import com.printbusinesskmp.models.LayoutUpdateRequest
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import com.printbusinesskmp.utils.labelUa
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import kotlin.math.roundToInt

@Composable
fun LayoutsScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var layouts by remember { mutableStateOf<List<Layout>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    var search by remember { mutableStateOf("") }
    var filterClientId by remember { mutableStateOf<String?>(null) }
    var filterStatus by remember { mutableStateOf<LayoutStatus?>(null) }

    var showEditor by remember { mutableStateOf(false) }
    var editingLayout by remember { mutableStateOf<Layout?>(null) }
    var editorName by remember { mutableStateOf("") }
    var editorClientId by remember { mutableStateOf<String?>(null) }
    var editorServiceType by remember { mutableStateOf(ServiceType.DTF) }
    var editorStatus by remember { mutableStateOf(LayoutStatus.FUTURE) }
    var editorWidthCm by remember { mutableStateOf("30") }
    var editorHeightCm by remember { mutableStateOf("20") }
    var editorDpi by remember { mutableStateOf("300") }
    var editorPreviewUrl by remember { mutableStateOf("") }
    var editorNotes by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf<String?>(null) }
    var editorSaving by remember { mutableStateOf(false) }

    var layoutIdToDelete by remember { mutableStateOf<String?>(null) }

    fun openEditor(target: Layout?) {
        editingLayout = target
        editorName = target?.name.orEmpty()
        editorClientId = target?.clientId
        editorServiceType = target?.serviceType ?: ServiceType.DTF
        editorStatus = target?.status ?: LayoutStatus.FUTURE
        editorWidthCm = target?.widthCm?.toString() ?: "30"
        editorHeightCm = target?.heightCm?.toString() ?: "20"
        editorDpi = target?.dpi?.toString() ?: "300"
        editorPreviewUrl = target?.previewUrl.orEmpty()
        editorNotes = target?.notes.orEmpty()
        editorError = null
        showEditor = true
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                clients = ApiClient.getClients()
                layouts = ApiClient.getLayouts()
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження макетів"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val clientById = clients.associateBy { it.id }
    val query = search.trim()

    val filtered = layouts
        .filter { layout ->
            val matchesSearch =
                query.isBlank() ||
                    layout.name.contains(query, ignoreCase = true) ||
                    (layout.notes?.contains(query, ignoreCase = true) ?: false) ||
                    (layout.clientId?.let { clientById[it]?.displayName }?.contains(query, ignoreCase = true) ?: false)

            val matchesClient = filterClientId == null || layout.clientId == filterClientId
            val matchesStatus = filterStatus == null || layout.status == filterStatus

            matchesSearch && matchesClient && matchesStatus
        }
        .sortedByDescending { it.updatedAt }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Макети",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkSlate
            )

            Button(
                onClick = { openEditor(null) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
            ) {
                Text("+ Новий макет", color = AppColors.White)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Пошук (назва, клієнт, примітки)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionalClientSelector(
                        label = "Клієнт",
                        clients = clients,
                        selectedId = filterClientId,
                        onSelect = { filterClientId = it },
                        noneLabel = "Усі",
                        modifier = Modifier.weight(1f)
                    )

                    OptionalStatusSelector(
                        label = "Категорія",
                        selected = filterStatus,
                        onSelect = { filterStatus = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        if (message != null) {
            Text(message ?: "", color = AppColors.Success, modifier = Modifier.padding(bottom = 8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardItemBg)
                    .padding(14.dp)
            ) {
                HeaderCell("Назва", Modifier.weight(1.6f))
                HeaderCell("Клієнт", Modifier.weight(1.5f))
                HeaderCell("Категорія", Modifier.weight(1f))
                HeaderCell("Сервіс", Modifier.weight(0.9f))
                HeaderCell("Розмір", Modifier.weight(1.2f))
                HeaderCell("DPI / PX", Modifier.weight(1.4f))
                HeaderCell("Оновлено", Modifier.weight(1f))
                HeaderCell("Дії", Modifier.weight(2.2f))
            }
            HorizontalDivider()

            if (filtered.isEmpty()) {
                Text(
                    text = "Немає макетів за обраними фільтрами",
                    color = AppColors.MediumGray,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(filtered) { layout ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.6f)) {
                                Text(layout.name, color = AppColors.DarkSlate, fontWeight = FontWeight.Medium)
                                layout.notes?.takeIf { it.isNotBlank() }?.let { note ->
                                    Text(note, color = AppColors.MediumGray, fontSize = 12.sp)
                                }
                            }

                            Text(
                                text = layout.clientId?.let { clientById[it]?.displayName } ?: "Без клієнта",
                                modifier = Modifier.weight(1.5f),
                                color = AppColors.DarkGrayText,
                                fontSize = 13.sp
                            )

                            Text(
                                text = layout.status.labelUa(),
                                modifier = Modifier.weight(1f),
                                color = AppColors.DarkGrayText,
                                fontSize = 12.sp
                            )

                            Text(
                                text = layout.serviceType.labelUa(),
                                modifier = Modifier.weight(0.9f),
                                color = AppColors.DarkSlate,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "${FormatUtils.formatDecimal(layout.widthCm)} × ${FormatUtils.formatDecimal(layout.heightCm)} см",
                                modifier = Modifier.weight(1.2f),
                                color = AppColors.DarkGrayText,
                                fontSize = 12.sp
                            )

                            Text(
                                text = "${layout.dpi} / ${pixelsFromCm(layout.widthCm, layout.dpi)}×${pixelsFromCm(layout.heightCm, layout.dpi)}",
                                modifier = Modifier.weight(1.4f),
                                color = AppColors.DarkGrayText,
                                fontSize = 12.sp
                            )

                            Text(
                                text = FormatUtils.formatDate(layout.updatedAt),
                                modifier = Modifier.weight(1f),
                                color = AppColors.MediumGray,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.weight(2.2f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                layout.previewUrl?.takeIf { it.isNotBlank() }?.let { previewUrl ->
                                    TextButton(onClick = { openPreview(previewUrl) }) {
                                        Text("Прев'ю", color = AppColors.PrimaryBlue, fontSize = 12.sp)
                                    }

                                    TextButton(onClick = { downloadPreview(layout.name, previewUrl) }) {
                                        Text("Скачати", color = AppColors.PrimaryBlue, fontSize = 12.sp)
                                    }
                                }

                                TextButton(onClick = { openEditor(layout) }) {
                                    Text("Редагувати", color = AppColors.PrimaryBlue, fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                ApiClient.createLayout(
                                                    LayoutCreateRequest(
                                                        clientId = layout.clientId,
                                                        name = "${layout.name} (копія)",
                                                        serviceType = layout.serviceType,
                                                        status = LayoutStatus.FUTURE,
                                                        widthCm = layout.widthCm,
                                                        heightCm = layout.heightCm,
                                                        dpi = layout.dpi,
                                                        previewUrl = layout.previewUrl,
                                                        notes = layout.notes
                                                    )
                                                )
                                                message = "Макет скопійовано для майбутнього проєкту"
                                                reload()
                                            } catch (e: Exception) {
                                                error = e.message ?: "Помилка копіювання макета"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Копія", color = AppColors.DarkSlate, fontSize = 12.sp)
                                }

                                TextButton(onClick = { layoutIdToDelete = layout.id }) {
                                    Text("Видалити", color = AppColors.Error, fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = {
                if (!editorSaving) {
                    showEditor = false
                }
            },
            title = {
                Text(if (editingLayout == null) "Новий макет" else "Редагування макета")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editorName,
                        onValueChange = { editorName = it },
                        label = { Text("Назва макета") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OptionalClientSelector(
                        label = "Клієнт",
                        clients = clients,
                        selectedId = editorClientId,
                        onSelect = { editorClientId = it },
                        noneLabel = "Без клієнта",
                        modifier = Modifier.fillMaxWidth()
                    )

                    EnumSelector(
                        label = "Тип друку",
                        values = ServiceType.entries,
                        selected = editorServiceType,
                        onSelect = { editorServiceType = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    EnumSelector(
                        label = "Категорія",
                        values = LayoutStatus.entries,
                        selected = editorStatus,
                        onSelect = { editorStatus = it },
                        textMapper = { it.labelUa() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editorWidthCm,
                            onValueChange = { editorWidthCm = it },
                            label = { Text("Ширина (см)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editorHeightCm,
                            onValueChange = { editorHeightCm = it },
                            label = { Text("Висота (см)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editorDpi,
                            onValueChange = { editorDpi = it },
                            label = { Text("DPI") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val parsedWidth = parseDoubleInput(editorWidthCm)
                    val parsedHeight = parseDoubleInput(editorHeightCm)
                    val parsedDpi = editorDpi.toIntOrNull()

                    if (parsedWidth != null && parsedHeight != null && parsedDpi != null && parsedWidth > 0 && parsedHeight > 0 && parsedDpi > 0) {
                        Text(
                            text = "Роздільна здатність: ${pixelsFromCm(parsedWidth, parsedDpi)}×${pixelsFromCm(parsedHeight, parsedDpi)} px",
                            color = AppColors.DarkSlate,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Площа: ${FormatUtils.formatDecimal((parsedWidth * parsedHeight) / 10000.0)} м²",
                            color = AppColors.MediumGray,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = editorPreviewUrl,
                        onValueChange = { editorPreviewUrl = it },
                        label = { Text("Посилання на прев'ю (URL або data URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            choosePreviewImage { dataUrl ->
                                editorPreviewUrl = dataUrl
                            }
                        }) {
                            Text("Додати файл прев'ю")
                        }

                        editorPreviewUrl.trim().takeIf { it.isNotBlank() }?.let { previewUrl ->
                            OutlinedButton(onClick = { openPreview(previewUrl) }) {
                                Text("Відкрити прев'ю")
                            }

                            OutlinedButton(
                                onClick = {
                                    downloadPreview(
                                        layoutName = editorName.ifBlank { "layout-preview" },
                                        previewUrl = previewUrl
                                    )
                                }
                            ) {
                                Text("Скачати прев'ю")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editorNotes,
                        onValueChange = { editorNotes = it },
                        label = { Text("Примітки") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editorError != null) {
                        Text(editorError ?: "", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedWidth = parseDoubleInput(editorWidthCm)
                        val parsedHeight = parseDoubleInput(editorHeightCm)
                        val parsedDpi = editorDpi.toIntOrNull()

                        if (editorName.isBlank()) {
                            editorError = "Вкажіть назву макета"
                            return@Button
                        }
                        if (parsedWidth == null || parsedWidth <= 0) {
                            editorError = "Ширина повинна бути більше нуля"
                            return@Button
                        }
                        if (parsedHeight == null || parsedHeight <= 0) {
                            editorError = "Висота повинна бути більше нуля"
                            return@Button
                        }
                        if (parsedDpi == null || parsedDpi <= 0) {
                            editorError = "DPI повинен бути більше нуля"
                            return@Button
                        }

                        editorSaving = true
                        editorError = null
                        error = null

                        scope.launch {
                            try {
                                if (editingLayout == null) {
                                    ApiClient.createLayout(
                                        LayoutCreateRequest(
                                            clientId = editorClientId,
                                            name = editorName,
                                            serviceType = editorServiceType,
                                            status = editorStatus,
                                            widthCm = parsedWidth,
                                            heightCm = parsedHeight,
                                            dpi = parsedDpi,
                                            previewUrl = editorPreviewUrl.ifBlank { null },
                                            notes = editorNotes.ifBlank { null }
                                        )
                                    )
                                    message = "Макет збережено"
                                } else {
                                    ApiClient.updateLayout(
                                        id = editingLayout!!.id,
                                        request = LayoutUpdateRequest(
                                            clientId = editorClientId,
                                            name = editorName,
                                            serviceType = editorServiceType,
                                            status = editorStatus,
                                            widthCm = parsedWidth,
                                            heightCm = parsedHeight,
                                            dpi = parsedDpi,
                                            previewUrl = editorPreviewUrl.ifBlank { null },
                                            notes = editorNotes.ifBlank { null }
                                        )
                                    )
                                    message = "Макет оновлено"
                                }

                                showEditor = false
                                reload()
                            } catch (e: Exception) {
                                editorError = e.message ?: "Помилка збереження макета"
                            } finally {
                                editorSaving = false
                            }
                        }
                    },
                    enabled = !editorSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                ) {
                    if (editorSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AppColors.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Зберегти", color = AppColors.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (!editorSaving) {
                        showEditor = false
                    }
                }) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (layoutIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { layoutIdToDelete = null },
            title = { Text("Видалити макет") },
            text = { Text("Підтвердьте видалення макета. Дію неможливо скасувати.") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = layoutIdToDelete ?: return@Button
                        scope.launch {
                            try {
                                ApiClient.deleteLayout(targetId)
                                layoutIdToDelete = null
                                message = "Макет видалено"
                                reload()
                            } catch (e: Exception) {
                                error = e.message ?: "Помилка видалення макета"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) {
                    Text("Видалити", color = AppColors.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { layoutIdToDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.DarkGrayText,
        fontSize = 13.sp,
        modifier = modifier
    )
}

@Composable
private fun OptionalClientSelector(
    label: String,
    clients: List<Client>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    noneLabel: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val selectedLabel = clients.find { it.id == selectedId }?.displayName ?: noneLabel
            Text("$label: $selectedLabel")
            TextButton(onClick = { expanded = true }) {
                Text("Обрати")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )

            clients.forEach { client ->
                DropdownMenuItem(
                    text = { Text(client.displayName) },
                    onClick = {
                        onSelect(client.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionalStatusSelector(
    label: String,
    selected: LayoutStatus?,
    onSelect: (LayoutStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("$label: ${selected?.labelUa() ?: "Усі"}")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Усі") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )

            LayoutStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.labelUa()) },
                    onClick = {
                        onSelect(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> EnumSelector(
    label: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    textMapper: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$label: ${textMapper(selected)}")
            TextButton(onClick = { expanded = true }) {
                Text("Обрати")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(textMapper(value)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun parseDoubleInput(raw: String): Double? {
    return raw.trim().replace(',', '.').toDoubleOrNull()
}

private fun pixelsFromCm(cm: Double, dpi: Int): Int {
    return ((cm / 2.54) * dpi).roundToInt()
}

private fun downloadPreview(layoutName: String, previewUrl: String) {
    val normalizedUrl = previewUrl.trim()
    if (normalizedUrl.isBlank()) return

    val extension = extractExtensionFromPreviewUrl(normalizedUrl)
    val fileName = buildPreviewFileName(layoutName, extension)

    val savePath = chooseSavePath(
        defaultFileName = fileName,
        dialogTitle = "Save preview image",
        extensions = extension?.let { listOf(it) } ?: emptyList(),
        description = "Image files"
    ) ?: return

    val bytes = loadPreviewBytes(normalizedUrl) ?: return
    runCatching {
        savePath.parent?.let { Files.createDirectories(it) }
        Files.write(savePath, bytes)
    }
}

private fun extractExtensionFromPreviewUrl(previewUrl: String): String? {
    if (!previewUrl.startsWith("data:", ignoreCase = true)) return null

    val mimeType = previewUrl
        .substringAfter("data:", "")
        .substringBefore(';', "")
        .lowercase()

    return when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/svg+xml" -> "svg"
        else -> null
    }
}

private fun buildPreviewFileName(layoutName: String, extension: String?): String {
    val sanitizedName = layoutName
        .trim()
        .ifBlank { "layout-preview" }
        .map { char ->
            if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_'
        }
        .joinToString("")
        .trim('_')
        .ifBlank { "layout-preview" }

    val safeExtension = extension ?: "png"
    return "$sanitizedName.$safeExtension"
}

private fun choosePreviewImage(onSelected: (String) -> Unit) {
    val imagePath = chooseImageFilePath() ?: return
    val bytes = runCatching { Files.readAllBytes(imagePath) }.getOrNull() ?: return
    val mimeType = mimeTypeFromExtension(imagePath.fileName.toString().substringAfterLast('.', "png"))
    val encoded = Base64.getEncoder().encodeToString(bytes)
    onSelected("data:$mimeType;base64,$encoded")
}

private fun openPreview(previewUrl: String) {
    val normalized = previewUrl.trim()
    if (normalized.isBlank()) return

    runCatching {
        val desktop = Desktop.getDesktop()
        if (normalized.startsWith("data:", ignoreCase = true)) {
            val bytes = decodeDataUrl(normalized) ?: return
            val extension = extractExtensionFromPreviewUrl(normalized) ?: "png"
            val tempFile = Files.createTempFile("layout-preview-", ".$extension")
            Files.write(tempFile, bytes)
            desktop.open(tempFile.toFile())
        } else {
            desktop.browse(URI(normalized))
        }
    }
}

private fun loadPreviewBytes(previewUrl: String): ByteArray? {
    return when {
        previewUrl.startsWith("data:", ignoreCase = true) -> decodeDataUrl(previewUrl)
        previewUrl.startsWith("file:", ignoreCase = true) -> runCatching {
            Files.readAllBytes(Paths.get(URI(previewUrl)))
        }.getOrNull()
        else -> runCatching {
            URI(previewUrl).toURL().openStream().use { it.readBytes() }
        }.getOrNull()
    }
}

private fun decodeDataUrl(dataUrl: String): ByteArray? {
    val encoded = dataUrl.substringAfter("base64,", "")
    if (encoded.isBlank()) {
        return null
    }
    return runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
}

private fun mimeTypeFromExtension(extension: String): String {
    return when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }
}
