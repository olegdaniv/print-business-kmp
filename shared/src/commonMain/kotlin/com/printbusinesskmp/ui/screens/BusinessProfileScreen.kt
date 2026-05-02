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
import com.printbusinesskmp.ui.components.EdrpouField
import com.printbusinesskmp.ui.components.IbanField
import com.printbusinesskmp.ui.components.IpnField
import com.printbusinesskmp.ui.components.MfoField
import com.printbusinesskmp.ui.components.PhoneField
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
                edrpou = profile.edrpou.filter { it.isDigit() }.take(8)
                ipn = profile.ipn.orEmpty().filter { it.isDigit() }.take(10)
                phone = profile.phone.orEmpty().filter { it.isDigit() }.take(10)
                address = profile.address
                iban = profile.iban.replace(" ", "").uppercase().take(29)
                bankName = profile.bankName.orEmpty()
                mfo = profile.mfo.orEmpty().filter { it.isDigit() }.take(6)
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
                    EdrpouField(
                        value = edrpou,
                        onValueChange = { edrpou = it; edrpouError = null; message = null },
                        label = "ЄДРПОУ *",
                        isError = edrpouError != null,
                        errorMessage = edrpouError,
                        modifier = Modifier.weight(1f)
                    )
                    IpnField(
                        value = ipn,
                        onValueChange = { ipn = it; ipnError = null; message = null },
                        label = "ІПН",
                        isError = ipnError != null,
                        errorMessage = ipnError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhoneField(
                        value = phone,
                        onValueChange = { phone = it; phoneError = null; message = null },
                        label = "Телефон *",
                        isError = phoneError != null,
                        errorMessage = phoneError,
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

                IbanField(
                    value = iban,
                    onValueChange = { iban = it; ibanError = null; message = null },
                    label = "IBAN *",
                    isError = ibanError != null,
                    errorMessage = ibanError,
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
                    MfoField(
                        value = mfo,
                        onValueChange = { mfo = it; mfoError = null; message = null },
                        label = "МФО",
                        isError = mfoError != null,
                        errorMessage = mfoError,
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
                            if (edrpou.length != 8) {
                                edrpouError = "Має бути рівно 8 цифр"
                                valid = false
                            }
                            if (ipn.isNotBlank() && ipn.length != 10) {
                                ipnError = "Має бути рівно 10 цифр"
                                valid = false
                            }
                            if (phone.length != 10 || !phone.startsWith("0")) {
                                phoneError = "Рівно 10 цифр, починається з 0"
                                valid = false
                            }
                            if (address.isBlank()) {
                                addressError = "Обов'язкове поле"
                                valid = false
                            }
                            if (!iban.startsWith("UA") || iban.length != 29) {
                                ibanError = "Формат: UA + 27 цифр (29 символів)"
                                valid = false
                            }
                            if (mfo.isNotBlank() && mfo.length != 6) {
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
                                            edrpou = edrpou,
                                            ipn = ipn.ifBlank { null },
                                            address = address.trim(),
                                            iban = iban,
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
