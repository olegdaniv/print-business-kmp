package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.platform.generateInvoiceToFolder
import com.printbusinesskmp.models.BusinessProfile
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.InvoiceCreateRequest
import com.printbusinesskmp.models.InvoiceLineRequest
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private data class LineItemState(
    val description: String = "",
    val unit: String = "шт.",
    val quantity: String = "1",
    val unitPrice: String = "0.00"
) {
    val lineTotal: Double
        get() = (quantity.toIntOrNull() ?: 0) * (unitPrice.toDoubleOrNull() ?: 0.0)
}

@Composable
fun InvoiceScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var profile by remember { mutableStateOf<BusinessProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var viewingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                invoices = ApiClient.getAllInvoices()
                clients = ApiClient.getClients()
                profile = ApiClient.getBusinessProfile()
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Рахунки",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )

                Button(
                    onClick = {
                        editingInvoice = null
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.railSelected)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Створити рахунок")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                return@Column
            }

            if (error != null) {
                Text(error ?: "", color = Color.Red)
                return@Column
            }

            if (message != null) {
                Text(message ?: "", color = AppColors.Success, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (invoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Text("Рахунків ще немає. Натисніть «Створити рахунок»", color = AppColors.MediumGray, fontSize = 15.sp)
                }
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.CardItemBg)
                        .padding(12.dp)
                ) {
                    TableHeader("Номер", Modifier.weight(1.5f))
                    TableHeader("Дата", Modifier.weight(1f))
                    TableHeader("Клієнт", Modifier.weight(2f))
                    TableHeader("Сума", Modifier.weight(1f))
                    TableHeader("Дійсний до", Modifier.weight(1f))
                    TableHeader("Дії", Modifier.weight(2.5f))
                }
                HorizontalDivider()

                LazyColumn {
                    items(invoices.sortedByDescending { it.issuedAt }) { invoice ->
                        InvoiceRow(
                            invoice = invoice,
                            onView = { viewingInvoice = invoice },
                            onDownload = {
                                scope.launch {
                                    try {
                                        val saved = generateInvoiceToFolder(invoice)
                                        message = "PDF збережено: $saved"
                                    } catch (e: Exception) {
                                        error = e.message ?: "Помилка збереження PDF"
                                    }
                                }
                            },
                            onEdit = {
                                editingInvoice = invoice
                                showForm = true
                            },
                            onDelete = { confirmDeleteId = invoice.id }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (confirmDeleteId != null) {
        val invoice = invoices.find { it.id == confirmDeleteId }
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Видалити рахунок") },
            text = { Text("Підтвердьте видалення рахунку ${invoice?.number}") },
            confirmButton = {
                TextButton(onClick = {
                    val idToDelete = confirmDeleteId
                    confirmDeleteId = null
                    if (idToDelete != null) {
                        scope.launch {
                            try {
                                ApiClient.deleteInvoice(idToDelete)
                                reload()
                            } catch (e: Exception) {
                                error = e.message ?: "Помилка видалення"
                            }
                        }
                    }
                }) { Text("Видалити", color = AppColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Скасувати") }
            }
        )
    }

    viewingInvoice?.let { inv ->
        InvoiceViewDialog(invoice = inv, onDismiss = { viewingInvoice = null })
    }

    if (showForm) {
        InvoiceFormDialog(
            editingInvoice = editingInvoice,
            clients = clients,
            profile = profile,
            onDismiss = { showForm = false },
            onSave = { request ->
                scope.launch {
                    try {
                        val existing = editingInvoice
                        if (existing != null) {
                            ApiClient.updateInvoice(existing.id, request)
                            message = "Рахунок оновлено"
                        } else {
                            ApiClient.createInvoice(request)
                            message = "Рахунок створено"
                        }
                        showForm = false
                        editingInvoice = null
                        reload()
                    } catch (e: Exception) {
                        error = e.message ?: "Помилка збереження"
                    }
                }
            }
        )
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, fontSize = 12.sp)
}

@Composable
private fun InvoiceRow(
    invoice: Invoice,
    onView: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(invoice.number, Modifier.weight(1.5f), color = AppColors.DarkSlate, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(FormatUtils.formatDate(invoice.issuedAt), Modifier.weight(1f), color = AppColors.MediumGray, fontSize = 12.sp)
        Text(invoice.client.name, Modifier.weight(2f), color = AppColors.DarkSlate, fontSize = 12.sp)
        Text(
            FormatUtils.formatCurrency(invoice.totalAmount),
            Modifier.weight(1f),
            color = AppColors.Success,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Text(
            invoice.validUntil?.let { FormatUtils.formatDate(it) } ?: "—",
            Modifier.weight(1f),
            color = AppColors.MediumGray,
            fontSize = 12.sp
        )
        Row(modifier = Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = "Переглянути", tint = DesktopColors.railSelected, modifier = Modifier.size(18.dp))
            }
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.railSelected)
            ) {
                Text("PDF", fontSize = 12.sp)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Редагувати", tint = AppColors.MediumGray, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = AppColors.Error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun InvoiceFormDialog(
    editingInvoice: Invoice?,
    clients: List<Client>,
    profile: BusinessProfile?,
    onDismiss: () -> Unit,
    onSave: (InvoiceCreateRequest) -> Unit
) {
    val isEdit = editingInvoice != null

    var selectedClientId by remember { mutableStateOf(editingInvoice?.clientId ?: editingInvoice?.client?.let { snap ->
        clients.find { it.displayName == snap.name }?.id
    } ?: "") }
    var clientDropdownOpen by remember { mutableStateOf(false) }
    var payer by remember { mutableStateOf(editingInvoice?.payer ?: "той самий") }
    var orderRef by remember { mutableStateOf(editingInvoice?.orderRef ?: "Без замовлення") }
    var discount by remember { mutableStateOf(editingInvoice?.discountAmount?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    var notes by remember { mutableStateOf(editingInvoice?.notes ?: "") }
    var validDays by remember { mutableStateOf("7") }

    val lineItems = remember {
        mutableStateListOf<LineItemState>().also { list ->
            if (editingInvoice != null) {
                editingInvoice.lines.forEach { line ->
                    list.add(LineItemState(
                        description = line.description,
                        unit = line.unit,
                        quantity = line.quantity.toString(),
                        unitPrice = line.unitPrice.toString()
                    ))
                }
            } else {
                list.add(LineItemState())
            }
        }
    }

    val subtotal by remember { derivedStateOf { lineItems.sumOf { it.lineTotal } } }
    val discountAmount by remember { derivedStateOf { discount.toDoubleOrNull() ?: 0.0 } }
    val total by remember { derivedStateOf { subtotal - discountAmount } }

    val selectedClient = clients.find { it.id == selectedClientId }
    val validationError = when {
        selectedClientId.isBlank() -> "Оберіть клієнта"
        lineItems.any { it.description.isBlank() } -> "Заповніть назву для кожної позиції"
        lineItems.any { (it.quantity.toIntOrNull() ?: 0) <= 0 } -> "Кількість має бути більше 0"
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(0.92f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEdit) "Редагувати рахунок ${editingInvoice!!.number}" else "Новий рахунок-фактура",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Supplier block (read-only) ───────────────────────────────
                    if (profile != null) {
                        SectionLabel("Постачальник")
                        Card(colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                InfoLine("ФОП: ${profile.ownerName}")
                                InfoLine("ЄДРПОУ: ${profile.edrpou}")
                                InfoLine("Адреса: ${profile.address}")
                                InfoLine("IBAN: ${profile.iban}")
                                val bankInfo = buildString {
                                    append(profile.bankName ?: "")
                                    profile.mfo?.takeIf { it.isNotBlank() }?.let { append(", МФО: $it") }
                                }
                                if (bankInfo.isNotBlank()) InfoLine(bankInfo)
                                val taxNote = profile.taxNote?.takeIf { it.isNotBlank() }
                                    ?: "Не є платником податку на прибуток на загальних підставах"
                                InfoLine(taxNote, color = AppColors.MediumGray)
                            }
                        }
                    } else {
                        Text("Увага: бізнес-профіль не заповнено. Будь ласка, заповніть профіль перед створенням рахунку.", color = AppColors.Error)
                    }

                    // ── Recipient block ─────────────────────────────────────────
                    SectionLabel("Одержувач та замовлення")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedClient?.displayName ?: "Оберіть клієнта...",
                                onValueChange = {},
                                label = { Text("Клієнт *") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    TextButton(onClick = { clientDropdownOpen = true }) { Text("Обрати") }
                                }
                            )
                            DropdownMenu(
                                expanded = clientDropdownOpen,
                                onDismissRequest = { clientDropdownOpen = false },
                                modifier = Modifier.widthIn(min = 300.dp)
                            ) {
                                clients.forEach { client ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(client.displayName, fontWeight = FontWeight.Medium)
                                                Text(client.phone, fontSize = 11.sp, color = AppColors.MediumGray)
                                            }
                                        },
                                        onClick = {
                                            selectedClientId = client.id
                                            clientDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = payer,
                            onValueChange = { payer = it },
                            label = { Text("Платник") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = orderRef,
                            onValueChange = { orderRef = it },
                            label = { Text("Замовлення") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = validDays,
                            onValueChange = { validDays = it.filter { c -> c.isDigit() } },
                            label = { Text("Дійсний (днів)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Line items ──────────────────────────────────────────────
                    SectionLabel("Позиції рахунку")

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(AppColors.CardItemBg).padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("№", Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Назва товару / послуги", Modifier.weight(3f).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Од.", Modifier.width(56.dp).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Кількість", Modifier.width(80.dp).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ціна без ПДВ", Modifier.width(110.dp).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Сума без ПДВ", Modifier.width(110.dp).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(36.dp))
                    }
                    HorizontalDivider()

                    lineItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("${index + 1}", Modifier.width(28.dp), fontSize = 12.sp, color = AppColors.MediumGray)

                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { lineItems[index] = item.copy(description = it) },
                                modifier = Modifier.weight(3f),
                                singleLine = true,
                                placeholder = { Text("Назва послуги/товару", fontSize = 11.sp) }
                            )

                            OutlinedTextField(
                                value = item.unit,
                                onValueChange = { lineItems[index] = item.copy(unit = it) },
                                modifier = Modifier.width(56.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.quantity,
                                onValueChange = { lineItems[index] = item.copy(quantity = it.filter { c -> c.isDigit() }) },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = item.unitPrice,
                                onValueChange = { lineItems[index] = item.copy(unitPrice = it.filter { c -> c.isDigit() || c == '.' }) },
                                modifier = Modifier.width(110.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            Text(
                                FormatUtils.formatDecimal(item.lineTotal),
                                modifier = Modifier.width(110.dp).padding(start = 8.dp),
                                fontWeight = FontWeight.Medium,
                                color = AppColors.DarkSlate,
                                fontSize = 13.sp
                            )

                            IconButton(
                                onClick = { if (lineItems.size > 1) lineItems.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Видалити", tint = AppColors.Error, modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider()
                    }

                    TextButton(
                        onClick = { lineItems.add(LineItemState()) },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Додати позицію")
                    }

                    // ── Totals ──────────────────────────────────────────────────
                    SectionLabel("Підсумки")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Знижка:", Modifier.widthIn(min = 160.dp), color = AppColors.MediumGray)
                            OutlinedTextField(
                                value = discount,
                                onValueChange = { discount = it.filter { c -> c.isDigit() || c == '.' } },
                                modifier = Modifier.widthIn(min = 120.dp, max = 160.dp),
                                singleLine = true,
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = { Text("грн", fontSize = 11.sp, color = AppColors.MediumGray) }
                            )
                        }
                        TotalLine("Разом без ПДВ:", FormatUtils.formatDecimal(subtotal) + " грн")
                        TotalLine("ПДВ:", "0.00 грн", subtle = true)
                        if (discountAmount > 0.0) TotalLine("Знижка:", "−${FormatUtils.formatDecimal(discountAmount)} грн", subtle = true)
                        TotalLine("Всього з ПДВ:", FormatUtils.formatDecimal(total) + " грн", bold = true)
                        Text(
                            FormatUtils.amountInUkrainianWords(total),
                            fontSize = 11.sp,
                            color = AppColors.MediumGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Примітки") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // ── Actions ─────────────────────────────────────────────────────
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (validationError != null) {
                        Text(validationError, color = AppColors.Error, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onDismiss) { Text("Скасувати") }
                    Button(
                        onClick = {
                            val lineRequests = lineItems.map { item ->
                                InvoiceLineRequest(
                                    description = item.description,
                                    unit = item.unit,
                                    quantity = item.quantity.toIntOrNull() ?: 1,
                                    unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0
                                )
                            }
                            onSave(
                                InvoiceCreateRequest(
                                    clientId = selectedClientId,
                                    lines = lineRequests,
                                    discountAmount = discount.toDoubleOrNull() ?: 0.0,
                                    notes = notes.trim().takeIf { it.isNotEmpty() },
                                    payer = payer.trim().ifBlank { "той самий" },
                                    orderRef = orderRef.trim().ifBlank { "Без замовлення" },
                                    validDays = validDays.toIntOrNull() ?: 7
                                )
                            )
                        },
                        enabled = validationError == null,
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.railSelected)
                    ) {
                        Text(if (isEdit) "Зберегти зміни" else "Створити рахунок")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceViewDialog(invoice: Invoice, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 780.dp).fillMaxSize(0.88f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(invoice.number, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Document header ──────────────────────────────────────────
                    Text(
                        "Рахунок-фактура № ${invoice.number} від ${formatLongDate(invoice.issuedAt)} р.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // ── Parties ──────────────────────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Supplier
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Постачальник", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AppColors.MediumGray)
                                Spacer(Modifier.height(2.dp))
                                Text("ФОП ${invoice.seller.ownerName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                ViewInfoLine("ЄДРПОУ: ${invoice.seller.taxId}")
                                ViewInfoLine("Адреса: ${invoice.seller.address}")
                                ViewInfoLine("IBAN: ${invoice.seller.iban}")
                                val bankInfo = buildString {
                                    if (invoice.seller.bankName.isNotBlank()) append(invoice.seller.bankName)
                                    invoice.seller.mfo?.takeIf { it.isNotBlank() }?.let {
                                        if (isNotEmpty()) append(", МФО: $it") else append("МФО: $it")
                                    }
                                }
                                if (bankInfo.isNotBlank()) ViewInfoLine(bankInfo)
                                val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
                                    ?: "Не є платником податку на прибуток на загальних підставах"
                                ViewInfoLine(taxNote, color = AppColors.MediumGray)
                            }
                        }
                        // Recipient
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Одержувач", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AppColors.MediumGray)
                                Spacer(Modifier.height(2.dp))
                                Text(invoice.client.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                ViewInfoLine("Телефон: ${invoice.client.phone}")
                                invoice.client.email?.takeIf { it.isNotBlank() }?.let { ViewInfoLine("Email: $it") }
                                Spacer(Modifier.height(4.dp))
                                ViewInfoLine("Платник: ${invoice.payer}")
                                ViewInfoLine("Замовлення: ${invoice.orderRef}")
                            }
                        }
                    }

                    // ── Line items table ─────────────────────────────────────────
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.White)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(AppColors.CardItemBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("№", Modifier.width(28.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText)
                                Text("Назва", Modifier.weight(3f).padding(start = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText)
                                Text("Од.", Modifier.width(44.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText)
                                Text("Кількість", Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("Ціна без ПДВ", Modifier.width(100.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                Text("Сума без ПДВ", Modifier.width(100.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                            HorizontalDivider()
                            invoice.lines.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${line.lineNumber}", Modifier.width(28.dp), fontSize = 12.sp, color = AppColors.MediumGray)
                                    Text(line.description, Modifier.weight(3f).padding(start = 4.dp), fontSize = 12.sp, color = AppColors.DarkSlate)
                                    Text(line.unit, Modifier.width(44.dp), fontSize = 12.sp, color = AppColors.MediumGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text("${line.quantity}", Modifier.width(70.dp), fontSize = 12.sp, color = AppColors.DarkSlate, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text(FormatUtils.formatDecimal(line.unitPrice), Modifier.width(100.dp), fontSize = 12.sp, color = AppColors.DarkSlate, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    Text(FormatUtils.formatDecimal(line.lineTotal), Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.DarkSlate, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                HorizontalDivider()
                            }
                        }
                    }

                    // ── Totals ───────────────────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ViewTotalLine("Разом без ПДВ:", "${FormatUtils.formatDecimal(invoice.subtotal)} грн")
                        if (invoice.discountAmount > 0.0) {
                            ViewTotalLine("Знижка:", "−${FormatUtils.formatDecimal(invoice.discountAmount)} грн")
                        }
                        ViewTotalLine("ПДВ:", "0.00 грн", subtle = true)
                        ViewTotalLine("Всього з ПДВ:", "${FormatUtils.formatDecimal(invoice.totalAmount)} грн", bold = true)
                    }

                    // ── Sum in words ─────────────────────────────────────────────
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Всього на суму: ${FormatUtils.amountInUkrainianWords(invoice.totalAmount)}",
                                fontSize = 12.sp, color = AppColors.DarkSlate
                            )
                            Text("ПДВ: 0.00 грн.", fontSize = 12.sp, color = AppColors.MediumGray)
                        }
                    }

                    // ── Footer ───────────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        invoice.validUntil?.let { until ->
                            Text(
                                "Рахунок дійсний до сплати до: ${FormatUtils.formatDate(until)}",
                                fontSize = 12.sp, color = AppColors.DarkSlate
                            )
                        }
                        Text(
                            "Виписав(ла): ФОП ${invoice.seller.ownerName}",
                            fontSize = 12.sp, color = AppColors.DarkSlate, fontWeight = FontWeight.Medium
                        )
                        invoice.notes?.takeIf { it.isNotBlank() }?.let {
                            Text("Примітка: $it", fontSize = 11.sp, color = AppColors.MediumGray)
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.railSelected)
                    ) {
                        Text("Закрити")
                    }
                }
            }
        }
    }
}

private fun formatLongDate(instant: kotlin.time.Instant): String {
    val dt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val months = mapOf(
        1 to "січня", 2 to "лютого", 3 to "березня", 4 to "квітня",
        5 to "травня", 6 to "червня", 7 to "липня", 8 to "серпня",
        9 to "вересня", 10 to "жовтня", 11 to "листопада", 12 to "грудня"
    )
    return "${dt.dayOfMonth} ${months[dt.monthNumber] ?: dt.monthNumber} ${dt.year}"
}

@Composable
private fun ViewInfoLine(text: String, color: Color = AppColors.DarkSlate) {
    Text(text, fontSize = 11.sp, color = color)
}

@Composable
private fun ViewTotalLine(label: String, value: String, bold: Boolean = false, subtle: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    val color = if (subtle) AppColors.MediumGray else AppColors.DarkSlate
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.widthIn(min = 160.dp), fontWeight = weight, color = color, fontSize = 13.sp)
        Text(value, fontWeight = weight, color = color, fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColors.DarkSlate)
}

@Composable
private fun InfoLine(text: String, color: Color = AppColors.DarkSlate) {
    Text(text, fontSize = 12.sp, color = color)
}

@Composable
private fun TotalLine(label: String, value: String, bold: Boolean = false, subtle: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    val color = if (subtle) AppColors.MediumGray else AppColors.DarkSlate
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.widthIn(min = 160.dp), fontWeight = weight, color = color, fontSize = 13.sp)
        Text(value, fontWeight = weight, color = color, fontSize = 13.sp)
    }
}