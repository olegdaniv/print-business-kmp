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
import com.printbusinesskmp.shared.resources.*
import com.printbusinesskmp.theme.AppColors.DarkSlate
import com.printbusinesskmp.theme.AppColors.PrimaryBlue
import com.printbusinesskmp.theme.AppColors.White
import com.printbusinesskmp.utils.ValidationErrorKeys
import com.printbusinesskmp.utils.ValidationUtils
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Maps validation error keys to string resources.
 */
@Composable
private fun getValidationErrorString(errorKey: String): String {
    return when (errorKey) {
        ValidationErrorKeys.NAME_REQUIRED -> stringResource(Res.string.validation_name_required)
        ValidationErrorKeys.PHONE_REQUIRED -> stringResource(Res.string.validation_phone_required)
        ValidationErrorKeys.PHONE_FORMAT -> stringResource(Res.string.validation_phone_format)
        ValidationErrorKeys.EMAIL_FORMAT -> stringResource(Res.string.validation_email_format)
        else -> errorKey // Fallback to the key itself if not found
    }
}

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
                    errorMessage = getString(Res.string.error_client_load_failed) + ": ${e.message}"
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
            } catch (e: Exception) {
                errorMessage = getString(Res.string.error_save_client) + ": ${e.message}"
                isSaving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = if (isEditMode) stringResource(Res.string.client_form_title_edit) else stringResource(Res.string.client_form_title_new),
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
                            text = stringResource(Res.string.client_form_label_name),
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
                            placeholder = { Text(stringResource(Res.string.client_form_placeholder_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["name"]?.let { errorKey ->
                            Text(
                                text = getValidationErrorString(errorKey),
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Phone field
                    Column {
                        Text(
                            text = stringResource(Res.string.client_form_label_phone),
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
                            placeholder = { Text(stringResource(Res.string.client_form_placeholder_phone)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("phone"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["phone"]?.let { errorKey ->
                            Text(
                                text = getValidationErrorString(errorKey),
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Email field
                    Column {
                        Text(
                            text = stringResource(Res.string.client_form_label_email),
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
                            placeholder = { Text(stringResource(Res.string.client_form_placeholder_email)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errors.containsKey("email"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White
                            )
                        )
                        errors["email"]?.let { errorKey ->
                            Text(
                                text = getValidationErrorString(errorKey),
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
                            Text(stringResource(Res.string.action_cancel))
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
                                Text(stringResource(Res.string.action_save), color = White)
                            }
                        }
                    }
                }
            }
        }
    }
}