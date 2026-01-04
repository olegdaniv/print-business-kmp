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
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.utils.ValidationUtils
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
                    errorMessage = "Failed to load client: ${e.message}"
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
                val client = Client(
                    id = clientId ?: "",
                    name = name,
                    phone = phone.replace(" ", ""),
                    email = email.ifBlank { null },
                    totalOrders = 0,
                    createdAt = Clock.System.now()
                )

                if (isEditMode) {
                    ApiClient.updateClient(clientId, client)
                } else {
                    ApiClient.createClient(client)
                }

                onNavigate(Screen.Clients)
            } catch (e: Exception) {
                errorMessage = e.toString()
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
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            color = Color(0xFF1E293B),
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
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
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
                            color = Color(0xFF1E293B),
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
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
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
                            color = Color(0xFF1E293B),
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
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Save", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}