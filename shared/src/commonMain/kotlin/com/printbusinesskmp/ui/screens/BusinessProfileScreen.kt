package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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

private const val DEFAULT_TAX_NOTE = "Не є платником податку на прибуток на загальних підставах"

@Composable
fun BusinessProfileScreen(@Suppress("UNUSED_PARAMETER") onNavigate: (Screen) -> Unit) {
    val scope = rememberCoroutineScope()

    var ownerName by remember { mutableStateOf("") }
    var edrpou by remember { mutableStateOf("") }
    var ipn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var mfo by remember { mutableStateOf("") }
    var taxNote by remember { mutableStateOf(DEFAULT_TAX_NOTE) }
    var certificateNumber by remember { mutableStateOf("") }

    var ownerNameError by remember { mutableStateOf<String?>(null) }
    var edrpouError by remember { mutableStateOf<String?>(null) }
    var ipnError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var ibanError by remember { mutableStateOf<String?>(null) }
    var mfoError by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var globalError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val profile = ApiClient.getBusinessProfile()
            if (profile != null) {
                ownerName = profile.ownerName
                edrpou = profile.edrpou
                ipn = profile.ipn.orEmpty()
                phone = profile.phone.orEmpty()
                address = profile.address
                iban = profile.iban
                bankName = profile.bankName.orEmpty()
                mfo = profile.mfo.orEmpty()
                taxNote = profile.taxNote.takeIf { !it.isNullOrBlank() } ?: DEFAULT_TAX_NOTE
                certificateNumber = profile.certificateNumber.orEmpty()
            }
        } catch (e: Exception) {
            globalError = e.message
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Профіль ФОП",
            fontSize = 28.sp,
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
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTitle("Основна інформація")

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it; ownerNameError = null; message = null },
                    label = { Text("ПІБ *") },
                    isError = ownerNameError != null,
                    supportingText = ownerNameError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = edrpou,
                        onValueChange = { if (it.length <= 8 || it.any { c -> !c.isDigit() }) { edrpou = it.filter { c -> c.isDigit() }.take(8); edrpouError = null; message = null } },
                        label = { Text("ЄДРПОУ * (8 цифр)") },
                        isError = edrpouError != null,
                        supportingText = edrpouError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ipn,
                        onValueChange = { ipn = it.filter { c -> c.isDigit() }.take(10); ipnError = null; message = null },
                        label = { Text("ІПН (10 цифр)") },
                        isError = ipnError != null,
                        supportingText = ipnError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneError = null; message = null },
                        label = { Text("Телефон * (0XXXXXXXXX)") },
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = certificateNumber,
                        onValueChange = { certificateNumber = it; message = null },
                        label = { Text("Номер свідоцтва") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; addressError = null; message = null },
                    label = { Text("Адреса *") },
                    isError = addressError != null,
                    supportingText = addressError?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SectionTitle("Банківські реквізити")

                OutlinedTextField(
                    value = iban,
                    onValueChange = { iban = it.replace(" ", "").uppercase().take(29); ibanError = null; message = null },
                    label = { Text("IBAN * (UA + 27 символів)") },
                    isError = ibanError != null,
                    supportingText = ibanError?.let { { Text(it, color = Color.Red) } },
                    placeholder = { Text("UA853996220000000026001233566 1") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it; message = null },
                        label = { Text("Назва банку") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mfo,
                        onValueChange = { mfo = it.filter { c -> c.isDigit() }.take(6); mfoError = null; message = null },
                        label = { Text("МФО (6 цифр)") },
                        isError = mfoError != null,
                        supportingText = mfoError?.let { { Text(it, color = Color.Red) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SectionTitle("Податкова інформація")

                OutlinedTextField(
                    value = taxNote,
                    onValueChange = { taxNote = it; message = null },
                    label = { Text("Примітка щодо податку") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(8.dp))

                if (globalError != null) {
                    Text(globalError ?: "", color = Color.Red, fontSize = 13.sp)
                }
                if (message != null) {
                    Text(message ?: "", color = AppColors.Success, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            var valid = true

                            if (ownerName.isBlank()) {
                                ownerNameError = "Обов'язкове поле"
                                valid = false
                            }
                            val edrpouDigits = edrpou.filter { it.isDigit() }
                            if (edrpouDigits.length != 8) {
                                edrpouError = "Має бути рівно 8 цифр"
                                valid = false
                            }
                            if (ipn.isNotBlank() && ipn.filter { it.isDigit() }.length != 10) {
                                ipnError = "Має бути рівно 10 цифр"
                                valid = false
                            }
                            val phoneDigits = phone.filter { it.isDigit() }
                            if (phoneDigits.length != 10) {
                                phoneError = "Має бути рівно 10 цифр (0XXXXXXXXX)"
                                valid = false
                            }
                            if (address.isBlank()) {
                                addressError = "Обов'язкове поле"
                                valid = false
                            }
                            val ibanNorm = iban.replace(" ", "").uppercase()
                            if (!ibanNorm.startsWith("UA") || ibanNorm.length != 29) {
                                ibanError = "Формат: UA + 27 символів (29 разом)"
                                valid = false
                            }
                            if (mfo.isNotBlank() && mfo.filter { it.isDigit() }.length != 6) {
                                mfoError = "Має бути рівно 6 цифр"
                                valid = false
                            }

                            if (!valid) return@Button

                            saving = true
                            globalError = null
                            message = null

                            scope.launch {
                                try {
                                    ApiClient.upsertBusinessProfile(
                                        BusinessProfileUpsertRequest(
                                            ownerName = ownerName.trim(),
                                            phone = phone.ifBlank { null },
                                            edrpou = edrpouDigits,
                                            ipn = ipn.ifBlank { null },
                                            address = address.trim(),
                                            iban = ibanNorm,
                                            bankName = bankName.ifBlank { null },
                                            mfo = mfo.ifBlank { null },
                                            taxNote = taxNote.ifBlank { null },
                                            certificateNumber = certificateNumber.ifBlank { null },
                                        )
                                    )
                                    message = "Профіль ФОП збережено"
                                } catch (e: Exception) {
                                    globalError = e.message ?: "Помилка збереження"
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.DarkSlate.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}
