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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.ui.components.HoverableRow
import com.printbusinesskmp.ui.components.SearchField
import com.printbusinesskmp.ui.components.SplitPane
import com.printbusinesskmp.ui.components.StatusFilterChips
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun DesktopClientsScreen(onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ClientType?>(null) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                clients = ApiClient.getClients()
                if (selectedClientId == null && clients.isNotEmpty()) {
                    selectedClientId = clients.first().id
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { load() }) { Text("Повторити") }
            }
        }
        return
    }

    val filtered = clients.filter { client ->
        val matchesSearch = search.isBlank() || run {
            val q = search.lowercase()
            client.displayName.lowercase().contains(q) ||
                client.phone.contains(q) ||
                client.email?.lowercase()?.contains(q) == true ||
                client.taxId?.contains(q) == true
        }
        val matchesType = typeFilter == null || client.type == typeFilter
        matchesSearch && matchesType
    }

    val selectedClient = selectedClientId?.let { id -> clients.find { it.id == id } }

    SplitPane(
        initialRatio = 0.35f,
        minLeftFraction = 0.25f,
        maxLeftFraction = 0.5f,
        leftContent = {
            ClientListPanel(
                clients = filtered,
                search = search,
                onSearchChange = { search = it },
                typeFilter = typeFilter,
                onTypeFilterChange = { typeFilter = it },
                selectedClientId = selectedClientId,
                onSelectClient = { selectedClientId = it },
                onNewClient = { onNavigate(Screen.ClientForm(null)) },
                onRefresh = { load() }
            )
        },
        rightContent = {
            if (selectedClient != null) {
                ClientDetailPanel(
                    client = selectedClient,
                    onEdit = { onNavigate(Screen.ClientForm(selectedClient.id)) },
                    onDelete = {
                        scope.launch {
                            try {
                                ApiClient.deleteClient(selectedClient.id)
                                selectedClientId = null
                                load()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }
                )
            } else {
                EmptyClientPanel()
            }
        }
    )
}

@Composable
private fun ClientListPanel(
    clients: List<Client>,
    search: String,
    onSearchChange: (String) -> Unit,
    typeFilter: ClientType?,
    onTypeFilterChange: (ClientType?) -> Unit,
    selectedClientId: String?,
    onSelectClient: (String) -> Unit,
    onNewClient: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Клієнти",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "Оновити", modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = onNewClient,
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.ContentPadding.let {
                        androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Додати", fontSize = 13.sp)
                }
            }
        }

        SearchField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = "Пошук за ім'ям, телефоном...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusFilterChips(
                values = ClientType.entries,
                selected = typeFilter,
                onSelect = onTypeFilterChange,
                labelMapper = { if (it == ClientType.PERSON) "Фізособи" else "Компанії" }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${clients.size} клієнтів",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (clients.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Немає клієнтів",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(clients, key = { it.id }) { client ->
                    ClientListItem(
                        client = client,
                        selected = client.id == selectedClientId,
                        onClick = { onSelectClient(client.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientListItem(
    client: Client,
    selected: Boolean,
    onClick: () -> Unit
) {
    HoverableRow(onClick = onClick, selected = selected) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (client.type == ClientType.PERSON) "Фізособа" else "Компанія",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = FormatUtils.formatPhone(client.phone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (client.orderCount > 0) {
                Text(
                    text = "${client.orderCount} замовлень",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ClientDetailPanel(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = client.displayName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (client.type == ClientType.PERSON) "Фізична особа" else "Юридична особа",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Редагувати")
            }
        }

        // Contact info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Контактна інформація", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                DetailField("Телефон", FormatUtils.formatPhone(client.phone))
                client.email?.let { DetailField("Email", it) }
                DetailField("Адреса", client.address)
                client.contactName?.let { DetailField("Контактна особа", it) }
            }
        }

        // Business info
        if (client.taxId != null || client.iban != null || client.bankName != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Реквізити", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    client.taxId?.let { DetailField("ЄДРПОУ / РНОКПП", it) }
                    client.iban?.let { DetailField("IBAN", it) }
                    client.bankName?.let { DetailField("Банк", it) }
                }
            }
        }

        // Discount
        client.discountPercent?.let { discount ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Знижка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${FormatUtils.formatDecimal(discount)}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DesktopColors.success
                    )
                }
            }
        }

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.printbusinesskmp.ui.components.StatCard(
                title = "Замовлень",
                value = client.orderCount.toString(),
                modifier = Modifier.weight(1f)
            )
            com.printbusinesskmp.ui.components.StatCard(
                title = "Клієнт з",
                value = FormatUtils.formatDate(client.createdAt),
                modifier = Modifier.weight(1f)
            )
        }

        // Notes
        client.notes?.takeIf { it.isNotBlank() }?.let { note ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Примітки", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Dates
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Створено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatDateTime(client.createdAt), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Оновлено", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatDateTime(client.updatedAt), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Delete
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Видалити клієнта", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    Text("Ця дія незворотна", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Видалити", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Видалити клієнта") },
            text = { Text("Підтвердьте видалення клієнта ${client.displayName}. Цю дію неможливо скасувати.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити", color = MaterialTheme.colorScheme.onError)
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

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyClientPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Оберіть клієнта",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
