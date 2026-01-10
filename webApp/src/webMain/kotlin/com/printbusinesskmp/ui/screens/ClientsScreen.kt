package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.shared.resources.*
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.theme.AppColors.CardItemBg
import com.printbusinesskmp.theme.AppColors.DarkGrayText
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.MediumGray
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClientsScreen(onNavigate: (Screen) -> Unit) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filteredClients = clients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true) ||
                (it.email?.contains(searchQuery, ignoreCase = true) == true)
    }

    fun loadClients() {
        scope.launch {
            try {
                isLoading = true
                clients = ApiClient.getClients()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = getString(Res.string.error_client_load_failed) + ": ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadClients()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.nav_clients),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )

            Button(
                onClick = { onNavigate(Screen.ClientDetail(null)) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(stringResource(Res.string.clients_add_button), color = White)
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(Res.string.clients_search_placeholder)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White
            )
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                fontSize = 16.sp
            )
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
                        TableHeaderCell(stringResource(Res.string.table_header_name), Modifier.weight(2f))
                        TableHeaderCell(stringResource(Res.string.table_header_phone), Modifier.weight(1.5f))
                        TableHeaderCell(stringResource(Res.string.table_header_email), Modifier.weight(2f))
                        TableHeaderCell(stringResource(Res.string.table_header_total_orders), Modifier.weight(1f))
                        TableHeaderCell(stringResource(Res.string.table_header_actions), Modifier.weight(1f))
                    }

                    HorizontalDivider()

                    // Table Rows
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filteredClients) { client ->
                            ClientRow(
                                client = client,
                                onEdit = { onNavigate(Screen.ClientDetail(client.id)) },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            ApiClient.deleteClient(client.id)
                                            loadClients()
                                        } catch (e: Exception) {
                                            errorMessage = getString(Res.string.error_delete_client) + ": ${e.message}"
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
private fun ClientRow(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = client.name,
            fontSize = 14.sp,
            color = DarkSlate,
            modifier = Modifier.weight(2f)
        )

        Text(
            text = FormatUtils.formatPhone(client.phone),
            fontSize = 14.sp,
            color = MediumGray,
            modifier = Modifier.weight(1.5f)
        )

        Text(
            text = client.email ?: stringResource(Res.string.clients_empty_indicator),
            fontSize = 14.sp,
            color = MediumGray,
            modifier = Modifier.weight(2f)
        )

        Text(
            text = client.totalOrders.toString(),
            fontSize = 14.sp,
            color = DarkSlate,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onEdit) {
                Text(stringResource(Res.string.action_edit), color = PrimaryBlue)
            }

            TextButton(onClick = { showDeleteDialog = true }) {
                Text(stringResource(Res.string.action_delete), color = AppColors.Error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.confirm_delete_client_title)) },
            text = { Text(stringResource(Res.string.confirm_delete_client_message, client.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.action_delete), color = AppColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}