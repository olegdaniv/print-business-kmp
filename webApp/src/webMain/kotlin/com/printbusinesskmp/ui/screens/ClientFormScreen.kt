package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.shared.resources.Res
import com.printbusinesskmp.shared.resources.action_edit
import com.printbusinesskmp.shared.resources.error_client_load_failed
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.ValidationUtils
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClientFormScreen(
    clientId: String?,
    onNavigate: (Screen) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(clientId != null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isEditMode = clientId != null

    LaunchedEffect(clientId) {
        if (clientId != null) {
            scope.launch {
                try {
                    val client = ApiClient.getClient(clientId)
                    name = client.name
                    phone = client.phone
                    email = client.email ?: ""
                    isLoading = false
                } catch (e: Exception) {
                    errorMessage = getString(Res.string.error_client_load_failed) + " ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    fun handleSave() {
        val validationErrors = ValidationUtils.validateClientForm(name, phone, email)
        if (validationErrors.isNotEmpty()) {
            errors = validationErrors
            return
        }

        scope.launch {
            try {
                isSaving = true

                if (isEditMode) {
                    val request = ClientUpdateRequest(
                        name = name,
                        phone = phone.replace(" ", ""),
                        email = email.ifBlank { null }
                    )
                    ApiClient.updateClient(clientId, request)
                } else {
                    val request = ClientCreateRequest(
                        name = name,
                        phone = phone.replace(" ", ""),
                        email = email.ifBlank { null }
                    )
                    ApiClient.createClient(request)
                }

                onNavigate(Screen.Clients)
            } catch (_: Exception) {
                errorMessage = "Failed to save client"
                isSaving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = if (isEditMode) "Edit Client" else "New Client",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Name field
                    Column {
                        Text(
                            text = "Name *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errors = errors - "name"
                            },
                            placeholder = { Text("Enter client name") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["name"]?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Phone field
                    Column {
                        Text(
                            text = "Phone *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                errors = errors - "phone"
                            },
                            placeholder = { Text("+380XXXXXXXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("phone"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["phone"]?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Email field
                    Column {
                        Text(
                            text = "Email",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errors = errors - "email"
                            },
                            placeholder = { Text("Enter email address") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("email"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["email"]?.let {
                            Text(
                                text = it,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigate(Screen.Clients) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { handleSave() },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = White
                                )
                            } else {
                                Text("Save", color = White)
                            }
                        }
                    }
                }
            }
        }
    }
}