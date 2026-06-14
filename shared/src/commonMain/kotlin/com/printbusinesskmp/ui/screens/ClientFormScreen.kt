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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientDelivery
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.models.DeliveryType
import com.printbusinesskmp.navigation.Screen
import com.printbusinesskmp.theme.AppColors
import com.printbusinesskmp.ui.components.EdrpouField
import com.printbusinesskmp.ui.components.IbanField
import com.printbusinesskmp.ui.components.IpnField
import com.printbusinesskmp.ui.components.LabeledDropdown
import com.printbusinesskmp.ui.components.PhoneField
import kotlinx.coroutines.launch

private fun DeliveryType.displayName(): String = when (this) {
    DeliveryType.NOVA_POSHTA_BRANCH -> "Відділення Нової Пошти"
    DeliveryType.NOVA_POSHTA_ADDRESS -> "Адресна доставка НП"
    DeliveryType.DIRECT_ADDRESS -> "Пряма адреса"
}

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

    // Delivery state
    var deliveryType by remember { mutableStateOf<DeliveryType?>(null) }
    var deliveryCity by remember { mutableStateOf("") }
    var deliveryBranch by remember { mutableStateOf("") }
    var deliveryStreet by remember { mutableStateOf("") }
    var deliveryBuilding by remember { mutableStateOf("") }
    var deliveryFreeAddress by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(editMode) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var ibanError by remember { mutableStateOf<String?>(null) }
    var taxIdError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clientId) {
        if (clientId != null) {
            try {
                val client = ApiClient.getClient(clientId)
                type = client.type
                displayName = client.displayName
                contactName = client.contactName.orEmpty()
                email = client.email.orEmpty()
                phone = client.phone.filter { it.isDigit() }.take(10)
                taxId = client.taxId.orEmpty().filter { it.isDigit() }.take(10)
                address = client.address
                iban = client.iban.orEmpty().replace(" ", "").uppercase().take(29)
                bankName = client.bankName.orEmpty()
                notes = client.notes.orEmpty()
                client.delivery?.let { d ->
                    deliveryType = d.type
                    deliveryCity = d.city.orEmpty()
                    deliveryBranch = d.branch.orEmpty()
                    deliveryStreet = d.street.orEmpty()
                    deliveryBuilding = d.building.orEmpty()
                    deliveryFreeAddress = d.freeAddress.orEmpty()
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun buildDelivery(): ClientDelivery? = deliveryType?.let { dt ->
        ClientDelivery(
            type = dt,
            city = deliveryCity.ifBlank { null },
            branch = deliveryBranch.ifBlank { null },
            street = deliveryStreet.ifBlank { null },
            building = deliveryBuilding.ifBlank { null },
            freeAddress = deliveryFreeAddress.ifBlank { null }
        )
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                // ── Client type ───────────────────────────────────────────
                LabeledDropdown(
                    label = "Тип клієнта",
                    selectedText = if (type == ClientType.PERSON) "Фізособа" else "Компанія",
                    options = listOf(ClientType.PERSON, ClientType.COMPANY),
                    optionLabel = { if (it == ClientType.PERSON) "Фізособа" else "Компанія" },
                    onSelect = { selected ->
                        type = selected
                        taxId = if (selected == ClientType.COMPANY) taxId.take(8) else taxId.take(10)
                        taxIdError = null
                    }
                )

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
                PhoneField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = null },
                    label = "Телефон",
                    isError = phoneError != null,
                    errorMessage = phoneError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == ClientType.COMPANY) {
                    EdrpouField(
                        value = taxId,
                        onValueChange = { taxId = it; taxIdError = null },
                        label = "ЄДРПОУ",
                        isError = taxIdError != null,
                        errorMessage = taxIdError,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    IpnField(
                        value = taxId,
                        onValueChange = { taxId = it; taxIdError = null },
                        label = "РНОКПП",
                        isError = taxIdError != null,
                        errorMessage = taxIdError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адреса") },
                    modifier = Modifier.fillMaxWidth()
                )
                IbanField(
                    value = iban,
                    onValueChange = { iban = it; ibanError = null },
                    label = "IBAN",
                    isError = ibanError != null,
                    errorMessage = ibanError,
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

                // ── Delivery section ──────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Доставка",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.DarkSlate.copy(alpha = 0.6f)
                )

                LabeledDropdown(
                    label = "Тип доставки",
                    selectedText = deliveryType?.displayName() ?: "Не вказано",
                    options = listOf<DeliveryType?>(null) + DeliveryType.entries,
                    optionLabel = { it?.displayName() ?: "Не вказано" },
                    onSelect = { dt ->
                        deliveryType = dt
                        if (dt == null) {
                            deliveryCity = ""; deliveryBranch = ""
                            deliveryStreet = ""; deliveryBuilding = ""
                            deliveryFreeAddress = ""
                        }
                    }
                )

                when (deliveryType) {
                    DeliveryType.NOVA_POSHTA_BRANCH -> {
                        OutlinedTextField(
                            value = deliveryCity,
                            onValueChange = { deliveryCity = it },
                            label = { Text("Місто") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = deliveryBranch,
                            onValueChange = { deliveryBranch = it },
                            label = { Text("Відділення (напр. Відділення №5)") },
                            placeholder = { Text("Відділення №5") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    DeliveryType.NOVA_POSHTA_ADDRESS -> {
                        OutlinedTextField(
                            value = deliveryCity,
                            onValueChange = { deliveryCity = it },
                            label = { Text("Місто") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = deliveryStreet,
                                onValueChange = { deliveryStreet = it },
                                label = { Text("Вулиця") },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = deliveryBuilding,
                                onValueChange = { deliveryBuilding = it },
                                label = { Text("Будинок") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    DeliveryType.DIRECT_ADDRESS -> {
                        OutlinedTextField(
                            value = deliveryFreeAddress,
                            onValueChange = { deliveryFreeAddress = it },
                            label = { Text("Адреса доставки") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    null -> Unit
                }

                // ── Error + actions ───────────────────────────────────────
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
                            var valid = true
                            if (displayName.isBlank() || address.isBlank()) {
                                error = "Заповніть обов'язкові поля: назва, адреса"
                                valid = false
                            }
                            if (phone.isBlank() || phone.length != 10 || !phone.startsWith("0")) {
                                phoneError = "Рівно 10 цифр, починається з 0"
                                valid = false
                            }
                            if (iban.isNotBlank() && (!iban.startsWith("UA") || iban.length != 29)) {
                                ibanError = "Формат: UA + 27 цифр (29 символів)"
                                valid = false
                            }
                            if (taxId.isNotBlank()) {
                                if (type == ClientType.COMPANY && taxId.length != 8) {
                                    taxIdError = "ЄДРПОУ: рівно 8 цифр"
                                    valid = false
                                } else if (type == ClientType.PERSON && taxId.length != 10) {
                                    taxIdError = "РНОКПП: рівно 10 цифр"
                                    valid = false
                                }
                            }
                            if (!valid) return@Button

                            saving = true
                            error = null

                            scope.launch {
                                try {
                                    val delivery = buildDelivery()
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
                                                notes = notes.ifBlank { null },
                                                delivery = delivery
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
                                                notes = notes.ifBlank { null },
                                                delivery = delivery
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