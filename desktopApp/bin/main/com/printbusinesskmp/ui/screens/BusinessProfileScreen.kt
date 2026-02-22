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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun BusinessProfileScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var taxPercent by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val profile = ApiClient.getBusinessProfile()
            if (profile != null) {
                ownerName = profile.ownerName
                email = profile.email.orEmpty()
                phone = profile.phone.orEmpty()
                taxId = profile.taxId
                address = profile.address
                iban = profile.iban
                bankName = profile.bankName
                taxPercent = profile.taxPercent.toString()
                notes = profile.notes.orEmpty()
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column {
        Text(
            text = "Профіль ФОП",
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
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Назва ФОП / ПІБ") },
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
                    label = { Text("РНОКПП / ЄДРПОУ") },
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
                    value = taxPercent,
                    onValueChange = { taxPercent = it },
                    label = { Text("Податок %") },
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
                if (message != null) {
                    Text(message ?: "", color = AppColors.Success)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            val parsedTax = taxPercent.toDoubleOrNull()
                            if (ownerName.isBlank() || phone.isBlank() || taxId.isBlank() || address.isBlank() || iban.isBlank() || bankName.isBlank() || parsedTax == null || parsedTax < 0) {
                                error = "Заповніть всі обов'язкові поля коректно"
                                return@Button
                            }

                            saving = true
                            error = null
                            message = null

                            scope.launch {
                                try {
                                    ApiClient.upsertBusinessProfile(
                                        BusinessProfileUpsertRequest(
                                            ownerName = ownerName,
                                            email = email.ifBlank { null },
                                            phone = phone,
                                            taxId = taxId,
                                            address = address,
                                            iban = iban,
                                            bankName = bankName,
                                            taxPercent = parsedTax,
                                            notes = notes.ifBlank { null }
                                        )
                                    )
                                    message = "Профіль ФОП збережено"
                                } catch (e: Exception) {
                                    error = e.message ?: "Помилка збереження"
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving,
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
