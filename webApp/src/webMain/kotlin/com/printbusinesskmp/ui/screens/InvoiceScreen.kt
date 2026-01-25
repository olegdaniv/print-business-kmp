package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.CardItemBg
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.Success
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun InvoiceScreen(onNavigate: (Screen) -> Unit) {
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                invoices = ApiClient.getAllInvoices()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Помилка завантаження рахунків: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Рахунки",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red,
                    fontSize = 16.sp
                )
            }
        } else if (invoices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Рахунків ще немає",
                    fontSize = 18.sp,
                    color = DarkGrayText
                )
            }
        } else {
            // Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardItemBg)
                            .padding(16.dp)
                    ) {
                        TableHeaderCell("№ Рахунку", Modifier.weight(1f))
                        TableHeaderCell("Дата", Modifier.weight(1.2f))
                        TableHeaderCell("Клієнт", Modifier.weight(2f))
                        TableHeaderCell("Сума", Modifier.weight(1f))
                        TableHeaderCell("Дії", Modifier.weight(1.5f))
                    }

                    HorizontalDivider()

                    // Table Rows
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(invoices) { invoice ->
                            InvoiceRow(
                                invoice = invoice,
                                onDownload = { downloadUrl ->
                                    // Open download URL in new tab
                                    kotlinx.browser.window.open(downloadUrl, "_blank")
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            invoice.id
                                            ApiClient.deleteInvoice(invoice.id)
                                            loadData()
                                        } catch (e: Exception) {
                                            errorMessage = "Помилка видалення рахунку: ${e.message}"
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = DarkGrayText,
        modifier = modifier
    )
}

@Composable
private fun InvoiceRow(
    invoice: Invoice,
    onDownload: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val downloadUrl = ApiClient.getInvoiceDownloadUrl(invoice.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice Number
        Text(
            text = "№${invoice.number}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkSlate,
            modifier = Modifier.weight(1f)
        )

        // Date
        Text(
            text = formatDate(invoice.date),
            fontSize = 14.sp,
            color = DarkSlate,
            modifier = Modifier.weight(1.2f)
        )

        // Client Name
        Text(
            text = invoice.client.name,
            fontSize = 14.sp,
            color = DarkSlate,
            modifier = Modifier.weight(2f)
        )

        // Total Amount
        Text(
            text = FormatUtils.formatCurrency(invoice.totalAmount, "грн"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Success,
            modifier = Modifier.weight(1f)
        )

        // Actions
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Download button
            Button(
                onClick = { onDownload(downloadUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Завантажити PDF", fontSize = 12.sp, color = White)
            }

            // Delete button
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Error
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Видалити", fontSize = 12.sp)
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Підтвердження видалення") },
            text = { Text("Ви впевнені, що хочете видалити рахунок №${invoice.number}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) {
                    Text("Видалити", color = White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

private fun formatDate(instant: kotlin.time.Instant): String {
    val instant =
        kotlin.time.Instant.fromEpochMilliseconds(instant.toEpochMilliseconds())
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.day.toString().padStart(2, '0')}.${
        localDateTime.month.toString().padStart(2, '0')
    }.${localDateTime.year}"
}
