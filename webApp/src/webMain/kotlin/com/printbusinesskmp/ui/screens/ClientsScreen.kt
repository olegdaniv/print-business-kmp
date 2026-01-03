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
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch

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
                errorMessage = "Failed to load clients: ${e.message}"
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
                text = "Clients",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Button(
                onClick = { onNavigate(Screen.ClientDetail(null)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("+ Add Client", color = Color.White)
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search clients...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
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
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(16.dp)
                    ) {
                        TableHeaderCell("Name", Modifier.weight(2f))
                        TableHeaderCell("Phone", Modifier.weight(1.5f))
                        TableHeaderCell("Email", Modifier.weight(2f))
                        TableHeaderCell("Total Orders", Modifier.weight(1f))
                        TableHeaderCell("Actions", Modifier.weight(1f))
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
                                            errorMessage = "Failed to delete client: ${e.message}"
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
        color = Color(0xFF475569),
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
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(2f)
        )

        Text(
            text = FormatUtils.formatPhone(client.phone),
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(1.5f)
        )

        Text(
            text = client.email ?: "-",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(2f)
        )

        Text(
            text = client.totalOrders.toString(),
            fontSize = 14.sp,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onEdit) {
                Text("Edit", color = Color(0xFF3B82F6))
            }

            TextButton(onClick = { showDeleteDialog = true }) {
                Text("Delete", color = Color(0xFFEF4444))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Client") },
            text = { Text("Are you sure you want to delete ${client.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}