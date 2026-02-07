package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.browser.window
import kotlinx.coroutines.launch

@Composable
fun InvoiceScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                invoices = ApiClient.getAllInvoices()
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column {
        Text(
            text = "Рахунки",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.DarkSlate,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        if (invoices.isEmpty()) {
            Text("Рахунків ще немає", color = AppColors.MediumGray)
            return@Column
        }

        Card(colors = CardDefaults.cardColors(containerColor = AppColors.White), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardItemBg)
                    .padding(14.dp)
            ) {
                Header("Номер", Modifier.weight(1.5f))
                Header("Дата", Modifier.weight(1f))
                Header("Клієнт", Modifier.weight(2f))
                Header("Сума", Modifier.weight(1f))
                Header("Дії", Modifier.weight(2f))
            }
            HorizontalDivider()

            LazyColumn {
                items(invoices.sortedByDescending { it.issuedAt }) { invoice ->
                    InvoiceRow(
                        invoice = invoice,
                        onDownload = {
                            window.open(ApiClient.getInvoiceDownloadUrl(invoice.id), "_blank")
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    ApiClient.deleteInvoice(invoice.id)
                                    reload()
                                } catch (e: Exception) {
                                    error = e.message ?: "Помилка видалення"
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

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, fontWeight = FontWeight.SemiBold, color = AppColors.DarkGrayText, fontSize = 13.sp)
}

@Composable
private fun InvoiceRow(
    invoice: Invoice,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(invoice.number, Modifier.weight(1.5f), color = AppColors.DarkSlate)
        Text(FormatUtils.formatDate(invoice.issuedAt), Modifier.weight(1f), color = AppColors.MediumGray, fontSize = 13.sp)
        Text(invoice.client.name, Modifier.weight(2f), color = AppColors.DarkSlate, fontSize = 13.sp)
        Text(
            FormatUtils.formatCurrency(invoice.totalAmount),
            Modifier.weight(1f),
            color = AppColors.Success,
            fontWeight = FontWeight.Medium
        )

        Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
            ) {
                Text("PDF", color = AppColors.White)
            }

            OutlinedButton(onClick = { confirmDelete = true }) {
                Text("Видалити", color = AppColors.Error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити рахунок") },
            text = { Text("Підтвердьте видалення рахунку ${invoice.number}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Видалити", color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}
