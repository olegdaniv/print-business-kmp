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
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                clients = ApiClient.getClients()
            } catch (e: Exception) {
                error = e.message ?: "Помилка завантаження"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    val filtered = clients.filter { client ->
        client.displayName.contains(search, ignoreCase = true) ||
            client.phone.contains(search, ignoreCase = true) ||
            (client.email?.contains(search, ignoreCase = true) == true) ||
            (client.taxId?.contains(search, ignoreCase = true) == true) ||
            (client.iban?.contains(search, ignoreCase = true) == true) ||
            (client.bankName?.contains(search, ignoreCase = true) == true) ||
            client.address.contains(search, ignoreCase = true)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Клієнти",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkSlate
            )

            Button(
                onClick = { onNavigate(Screen.ClientForm(null)) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
            ) {
                Text("+ Додати", color = AppColors.White)
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Пошук") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        if (error != null) {
            Text(error ?: "", color = Color.Red)
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardItemBg)
                    .padding(14.dp)
            ) {
                HeaderCell("Назва", Modifier.weight(1.8f))
                HeaderCell("Email", Modifier.weight(1.6f))
                HeaderCell("Телефон", Modifier.weight(1.2f))
                HeaderCell("ЄДРПОУ", Modifier.weight(1.1f))
                HeaderCell("Адреса", Modifier.weight(1.8f))
                HeaderCell("Зам.", Modifier.weight(0.7f))
                HeaderCell("Дії", Modifier.weight(1.2f))
            }
            HorizontalDivider()

            LazyColumn {
                items(filtered) { client ->
                    ClientRow(
                        client = client,
                        onEdit = { onNavigate(Screen.ClientForm(client.id)) },
                        onDelete = {
                            scope.launch {
                                try {
                                    ApiClient.deleteClient(client.id)
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
private fun ClientRow(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(client.displayName, Modifier.weight(1.8f), color = AppColors.DarkSlate)
        Text(client.email.orEmpty(), Modifier.weight(1.6f), color = AppColors.MediumGray, fontSize = 13.sp)
        Text(client.phone, Modifier.weight(1.2f), color = AppColors.MediumGray, fontSize = 13.sp)
        Text(client.taxId.orEmpty(), Modifier.weight(1.1f), color = AppColors.MediumGray, fontSize = 13.sp)
        Text(client.address, Modifier.weight(1.8f), color = AppColors.MediumGray, fontSize = 13.sp)
        Text(client.orderCount.toString(), Modifier.weight(0.7f), color = AppColors.DarkSlate)

        Row(modifier = Modifier.weight(1.2f)) {
            TextButton(onClick = onEdit) {
                Text("Редагувати", color = AppColors.PrimaryBlue, fontSize = 12.sp)
            }
            TextButton(onClick = { confirmDelete = true }) {
                Text("Видалити", color = AppColors.Error, fontSize = 12.sp)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити клієнта") },
            text = { Text("Підтвердьте видалення клієнта ${client.displayName}") },
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
