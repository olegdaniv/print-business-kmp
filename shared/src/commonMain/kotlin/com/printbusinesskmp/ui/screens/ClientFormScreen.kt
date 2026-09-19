package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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

    fun save() {
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
        if (!valid) return

        saving = true
        error = null

        scope.launch {
            try {
                val delivery = buildDelivery()
                if (clientId != null) {
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
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).widthIn(max = 1000.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = if (editMode) "Редагування клієнта" else "Новий клієнт",
            subtitle = if (editMode) displayName.ifBlank { null } else "Поля з * обов'язкові"
        ) {
            TextButton(onClick = { onNavigate(Screen.Clients) }) { Text("Скасувати") }
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

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard(title = "Основне") {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(if (type == ClientType.COMPANY) "Назва компанії *" else "Ім'я *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Контактна особа") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionCard(title = "Контакти") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    PhoneField(
                        value = phone,
                        onValueChange = { phone = it; phoneError = null },
                        label = "Телефон *",
                        isError = phoneError != null,
                        errorMessage = phoneError,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адреса *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "Реквізити", subtitle = "Потрібні для рахунків і накладних") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    if (type == ClientType.COMPANY) {
                        EdrpouField(
                            value = taxId,
                            onValueChange = { taxId = it; taxIdError = null },
                            label = "ЄДРПОУ",
                            isError = taxIdError != null,
                            errorMessage = taxIdError,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        IpnField(
                            value = taxId,
                            onValueChange = { taxId = it; taxIdError = null },
                            label = "РНОКПП",
                            isError = taxIdError != null,
                            errorMessage = taxIdError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Банк") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                IbanField(
                    value = iban,
                    onValueChange = { iban = it; ibanError = null },
                    label = "IBAN",
                    isError = ibanError != null,
                    errorMessage = ibanError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "Доставка") {
                LabeledDropdown(
                    label = "Спосіб доставки",
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = deliveryCity,
                                onValueChange = { deliveryCity = it },
                                label = { Text("Місто") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = deliveryBranch,
                                onValueChange = { deliveryBranch = it },
                                label = { Text("Відділення") },
                                placeholder = { Text("Відділення №5") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    DeliveryType.NOVA_POSHTA_ADDRESS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = deliveryCity,
                                onValueChange = { deliveryCity = it },
                                label = { Text("Місто") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = deliveryStreet,
                                onValueChange = { deliveryStreet = it },
                                label = { Text("Вулиця") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = deliveryBuilding,
                                onValueChange = { deliveryBuilding = it },
                                label = { Text("Будинок") },
                                singleLine = true,
                                modifier = Modifier.width(140.dp)
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
            }

            SectionCard(title = "Примітки") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Будь-що корисне про клієнта") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = { onNavigate(Screen.Clients) }) { Text("Скасувати") }
                Button(onClick = { save() }, enabled = !saving, shape = RoundedCornerShape(8.dp)) {
                    Text(if (editMode) "Зберегти зміни" else "Створити клієнта")
                }
            }
        }
    }
}
