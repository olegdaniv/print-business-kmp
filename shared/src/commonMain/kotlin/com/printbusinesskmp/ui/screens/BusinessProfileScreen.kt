package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.printbusinesskmp.ui.components.InfoRow
import com.printbusinesskmp.ui.components.ScreenHeader
import com.printbusinesskmp.ui.components.SectionCard
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
import androidx.compose.material3.MaterialTheme
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
    var bankEdrpou by remember { mutableStateOf("") }
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
                edrpou = profile.edrpou.filter { it.isDigit() }.take(10)
                ipn = profile.ipn.orEmpty().filter { it.isDigit() }.take(10)
                phone = profile.phone.orEmpty().filter { it.isDigit() }.take(10)
                address = profile.address
                iban = profile.iban.replace(" ", "").uppercase().take(29)
                bankName = profile.bankName.orEmpty()
                bankEdrpou = profile.bankEdrpou.orEmpty().filter { it.isDigit() }.take(10)
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

    fun save() {
        var valid = true

        if (ownerName.isBlank()) {
            ownerNameError = "Обов'язкове поле"
            valid = false
        }
        if (edrpou.length != 8 && edrpou.length != 10) {
            edrpouError = "8 цифр (юр. особа) або 10 (ФОП)"
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

        if (!valid) {
            globalError = "Перевірте поля, позначені червоним"
            return
        }

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
                        bankEdrpou = bankEdrpou.ifBlank { null },
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
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Профіль ФОП",
            subtitle = "Ці дані потрапляють у рахунки та видаткові накладні"
        ) {
            message?.let { Text(it, color = AppColors.Success, fontSize = 13.sp) }
            Button(
                onClick = { save() },
                enabled = !saving && !loading,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Зберегти")
                }
            }
        }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        Column(
            modifier = Modifier.widthIn(max = 1200.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(
                    title = "Основна інформація",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it; ownerNameError = null; message = null },
                        label = { Text("ПІБ *") },
                        isError = ownerNameError != null,
                        supportingText = ownerNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        EdrpouField(
                            value = edrpou,
                            onValueChange = { edrpou = it; edrpouError = null; message = null },
                            label = "ЄДРПОУ *",
                            isError = edrpouError != null,
                            errorMessage = edrpouError,
                            allowFop = true,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
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
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; addressError = null; message = null },
                        label = { Text("Адреса *") },
                        isError = addressError != null,
                        supportingText = addressError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                SectionCard(
                    title = "Банківські реквізити",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    IbanField(
                        value = iban,
                        onValueChange = { iban = it; ibanError = null; message = null },
                        label = "IBAN *",
                        isError = ibanError != null,
                        errorMessage = ibanError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it; message = null },
                            label = { Text("Назва банку") },
                            singleLine = true,
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
                    OutlinedTextField(
                        value = bankEdrpou,
                        onValueChange = { bankEdrpou = it.filter { c -> c.isDigit() }.take(10); message = null },
                        label = { Text("ЄДРПОУ банку") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionCard(title = "Податкова інформація", subtitle = "Друкується внизу рахунку") {
                OutlinedTextField(
                    value = taxNote,
                    onValueChange = { taxNote = it; message = null },
                    label = { Text("Примітка щодо податку") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            SectionCard(title = "Як це виглядатиме в рахунку", subtitle = "Блок «Постачальник»") {
                InfoRow("Постачальник", ownerName.ifBlank { "—" })
                InfoRow("ЄДРПОУ", edrpou.ifBlank { "—" })
                if (ipn.isNotBlank()) InfoRow("ІПН", ipn)
                InfoRow("Адреса", address.ifBlank { "—" })
                InfoRow("IBAN", iban.ifBlank { "—" })
                InfoRow(
                    "Банк",
                    listOfNotNull(bankName.ifBlank { null }, mfo.ifBlank { null }?.let { "МФО $it" })
                        .joinToString(", ").ifBlank { "—" }
                )
            }

            if (globalError != null) {
                Text(globalError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}
