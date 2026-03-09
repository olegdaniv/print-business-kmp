package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(
    clientId: String?,
    onNavigate: (Screen) -> Unit
) {
    val scope = rememberCoroutineScope()
    val editMode = clientId != null

    var type by remember { mutableStateOf(ClientType.PERSON) }
    var displayName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(editMode) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        if (clientId != null) {
            try {
                val client = ApiClient.getClient(clientId)
                type = client.type
                displayName = client.displayName
                contactName = client.contactName.orEmpty()
                email = client.email.orEmpty()
                phone = client.phone
                taxId = client.taxId.orEmpty()
                address = client.address
                iban = client.iban.orEmpty()
                bankName = client.bankName.orEmpty()
                notes = client.notes.orEmpty()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    Column {
        Text(
            text = if (editMode) "Редагування клієнта" else "Новий клієнт",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.DarkSlate,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Тип: ${if (type == ClientType.PERSON) "Фізособа" else "Компанія"}")
                    TextButton(onClick = { typeExpanded = true }) {
                        Text("Змінити")
                    }
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Фізособа") },
                            onClick = {
                                type = ClientType.PERSON
                                typeExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Компанія") },
                            onClick = {
                                type = ClientType.COMPANY
                                typeExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Назва / Ім'я") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Контактна особа (опціонально)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    label = { Text("ЄДРПОУ / РНОКПП") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адреса") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iban,
                    onValueChange = { iban = it },
                    label = { Text("IBAN") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Банк") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примітки") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error ?: "", color = Color.Red)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { onNavigate(Screen.Clients) }, modifier = Modifier.weight(1f)) {
                        Text("Скасувати")
                    }
                    Button(
                        onClick = {
                            if (displayName.isBlank() || phone.isBlank() || address.isBlank()) {
                                error = "Заповніть обов'язкові поля: назва, телефон, адреса"
                                return@Button
                            }

                            saving = true
                            error = null

                            scope.launch {
                                try {
                                    if (editMode) {
                                        ApiClient.updateClient(
                                            clientId,
                                            ClientUpdateRequest(
                                                type = type,
                                                displayName = displayName,
                                                contactName = contactName.ifBlank { null },
                                                phone = phone,
                                                email = email.ifBlank { null },
                                                taxId = taxId.ifBlank { null },
                                                address = address,
                                                iban = iban.ifBlank { null },
                                                bankName = bankName.ifBlank { null },
                                                notes = notes.ifBlank { null }
                                            )
                                        )
                                    } else {
                                        ApiClient.createClient(
                                            ClientCreateRequest(
                                                type = type,
                                                displayName = displayName,
                                                contactName = contactName.ifBlank { null },
                                                phone = phone,
                                                email = email.ifBlank { null },
                                                taxId = taxId.ifBlank { null },
                                                address = address,
                                                iban = iban.ifBlank { null },
                                                bankName = bankName.ifBlank { null },
                                                notes = notes.ifBlank { null }
                                            )
                                        )
                                    }
                                    onNavigate(Screen.Clients)
                                } catch (e: Exception) {
                                    error = e.message ?: "Помилка збереження"
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AppColors.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Зберегти", color = AppColors.White)
                        }
                    }
                }
            }
        }
    }
}
