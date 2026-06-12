package com.printbusinesskmp.routes

import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.InvoiceClientSnapshot
import com.printbusinesskmp.models.InvoiceCreateRequest
import com.printbusinesskmp.models.InvoiceLine
import com.printbusinesskmp.models.InvoiceNumberFormatInfo
import com.printbusinesskmp.models.InvoiceNumberFormatUpdateRequest
import com.printbusinesskmp.models.InvoiceNumberOverrideRequest
import com.printbusinesskmp.models.InvoiceSellerSnapshot
import com.printbusinesskmp.models.OrderItem
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.repository.AppSettingsRepository
import com.printbusinesskmp.repository.BusinessProfileRepository
import com.printbusinesskmp.repository.ClientRepository
import com.printbusinesskmp.repository.InvoiceRepository
import com.printbusinesskmp.repository.OrderRepository
import com.printbusinesskmp.services.InvoiceGenerator
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.io.File
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private val invoiceRepository = InvoiceRepository()
private val orderRepository = OrderRepository()
private val clientRepository = ClientRepository()
private val businessProfileRepository = BusinessProfileRepository()
private val invoiceGenerator = InvoiceGenerator()
private val appSettingsRepository = AppSettingsRepository()

private const val INVOICE_NUMBER_TEMPLATE_KEY = "invoice_number_template"
private const val DEFAULT_NUMBER_TEMPLATE = "СФ-0000000"

/** Splits a template like "СФ-0000000" into the constant prefix and the digit count. */
private fun parseNumberTemplate(template: String): Pair<String, Int> {
    val padding = template.takeLastWhile { it == '0' }.length
    return template.dropLast(padding) to maxOf(padding, 1)
}

private suspend fun currentNumberTemplate(): String =
    appSettingsRepository.get(INVOICE_NUMBER_TEMPLATE_KEY)?.takeIf { it.isNotBlank() }
        ?: DEFAULT_NUMBER_TEMPLATE

private suspend fun nextInvoiceNumberFromSettings(): String {
    val (prefix, padding) = parseNumberTemplate(currentNumberTemplate())
    return invoiceRepository.nextInvoiceNumber(prefix, padding)
}

fun Route.configureInvoiceRoutes() {
    authenticate("app-jwt") {
        route("/api/invoices") {

            post {
                try {
                    val request = call.receive<InvoiceCreateRequest>()

                    val client = clientRepository.clientById(request.clientId)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))

                    val profile = businessProfileRepository.getProfile()
                        ?: return@post call.respond(
                            HttpStatusCode.PreconditionFailed,
                            mapOf("error" to "Business profile must be configured before invoice generation")
                        )

                    if (request.lines.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invoice must contain at least one line item")
                        )
                    }

                    val number = nextInvoiceNumberFromSettings()
                    val now = Clock.System.now()
                    val validUntil = now + request.validDays.days

                    val lines = request.lines.mapIndexed { index, lineReq ->
                        val lineTotal = lineReq.quantity * lineReq.unitPrice
                        InvoiceLine(
                            lineNumber = index + 1,
                            description = lineReq.description,
                            quantity = lineReq.quantity,
                            unit = lineReq.unit,
                            usedMeters = 0.0,
                            unitPrice = lineReq.unitPrice,
                            lineTotal = lineTotal
                        )
                    }

                    val subtotal = lines.sumOf { it.lineTotal }
                    val finalAmount = subtotal - request.discountAmount

                    val invoice = Invoice(
                        id = UUID.randomUUID().toString(),
                        number = number,
                        clientId = client.id,
                        issuedAt = now,
                        validUntil = validUntil,
                        payer = request.payer,
                        orderRef = request.orderRef,
                        seller = InvoiceSellerSnapshot(
                            ownerName = profile.ownerName,
                            taxId = profile.edrpou,
                            address = profile.address,
                            iban = profile.iban,
                            bankName = profile.bankName.orEmpty(),
                            taxPercent = profile.taxPercent,
                            taxNote = profile.taxNote,
                            mfo = profile.mfo,
                            ipn = profile.ipn,
                        ),
                        client = InvoiceClientSnapshot(
                            type = client.type,
                            name = client.displayName,
                            address = client.address,
                            phone = client.phone,
                            email = client.email
                        ),
                        lines = lines,
                        subtotal = subtotal,
                        discountAmount = request.discountAmount,
                        taxAmount = 0.0,
                        totalAmount = finalAmount,
                        finalAmount = finalAmount,
                        notes = request.notes
                    )

                    val saved = invoiceRepository.addInvoice(invoice)
                    val filePath = invoiceGenerator.generateInvoicePdf(saved)
                    val withFile = invoiceRepository.updateInvoiceFilePath(saved.id, filePath) ?: saved

                    call.respond(HttpStatusCode.Created, withFile)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Invoice creation failed", "details" to (e.message ?: "unknown error"))
                    )
                }
            }

            put("{id}") {
                try {
                    val id = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                    val existing = invoiceRepository.invoiceById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    val request = call.receive<InvoiceCreateRequest>()

                    val client = clientRepository.clientById(request.clientId)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))

                    if (request.lines.isEmpty()) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invoice must contain at least one line item")
                        )
                    }

                    val validUntil = existing.issuedAt + request.validDays.days

                    val lines = request.lines.mapIndexed { index, lineReq ->
                        val lineTotal = lineReq.quantity * lineReq.unitPrice
                        InvoiceLine(
                            lineNumber = index + 1,
                            description = lineReq.description,
                            quantity = lineReq.quantity,
                            unit = lineReq.unit,
                            usedMeters = 0.0,
                            unitPrice = lineReq.unitPrice,
                            lineTotal = lineTotal
                        )
                    }

                    val subtotal = lines.sumOf { it.lineTotal }
                    val finalAmount = subtotal - request.discountAmount

                    val updated = existing.copy(
                        clientId = client.id,
                        validUntil = validUntil,
                        payer = request.payer,
                        orderRef = request.orderRef,
                        client = InvoiceClientSnapshot(
                            type = client.type,
                            name = client.displayName,
                            address = client.address,
                            phone = client.phone,
                            email = client.email
                        ),
                        lines = lines,
                        subtotal = subtotal,
                        discountAmount = request.discountAmount,
                        taxAmount = 0.0,
                        totalAmount = finalAmount,
                        finalAmount = finalAmount,
                        notes = request.notes
                    )

                    val saved = invoiceRepository.updateInvoice(id, updated)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    existing.filePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                    val filePath = invoiceGenerator.generateInvoicePdf(saved)
                    val withFile = invoiceRepository.updateInvoiceFilePath(saved.id, filePath) ?: saved

                    call.respond(HttpStatusCode.OK, withFile)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Invoice update failed", "details" to (e.message ?: "unknown error"))
                    )
                }
            }

            post("/generate/{orderId}") {
                try {
                    val orderId = call.parameters["orderId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

                    val order = orderRepository.orderById(orderId)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))

                    val client = clientRepository.clientById(order.clientId)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))

                    val profile = businessProfileRepository.getProfile()
                        ?: return@post call.respond(
                            HttpStatusCode.PreconditionFailed,
                            mapOf("error" to "Business profile must be configured before invoice generation")
                        )

                    if (order.items.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Order must contain at least one item to generate invoice")
                        )
                    }

                    val number = nextInvoiceNumberFromSettings()
                    val now = Clock.System.now()

                    val lines = buildLinesFromOrderItems(order.items)

                    val subtotal = order.items.sumOf { it.price - it.taxAmount }
                    val taxAmount = order.items.sumOf { it.taxAmount }
                    val totalAmount = order.items.sumOf { it.price }

                    val invoice = Invoice(
                        id = UUID.randomUUID().toString(),
                        number = number,
                        orderId = order.id,
                        clientId = client.id,
                        issuedAt = now,
                        validUntil = now + 7.days,
                        payer = "той самий",
                        orderRef = "Замовлення ${order.id.take(8)}",
                        seller = InvoiceSellerSnapshot(
                            ownerName = profile.ownerName,
                            taxId = profile.edrpou,
                            address = profile.address,
                            iban = profile.iban,
                            bankName = profile.bankName.orEmpty(),
                            taxPercent = profile.taxPercent,
                            taxNote = profile.taxNote,
                            mfo = profile.mfo,
                            ipn = profile.ipn,
                        ),
                        client = InvoiceClientSnapshot(
                            type = client.type,
                            name = client.displayName,
                            address = client.address,
                            phone = client.phone,
                            email = client.email
                        ),
                        lines = lines,
                        subtotal = subtotal,
                        taxAmount = taxAmount,
                        totalAmount = totalAmount,
                        notes = order.notes
                    )

                    val saved = invoiceRepository.addInvoice(invoice)
                    val filePath = invoiceGenerator.generateInvoicePdf(saved)
                    val withFile = invoiceRepository.updateInvoiceFilePath(saved.id, filePath) ?: saved

                    call.respond(HttpStatusCode.Created, withFile)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid invoice request")))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Invoice generation failed", "details" to (e.message ?: "unknown error"))
                    )
                }
            }

            post("{id}/regenerate") {
                try {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                    val existing = invoiceRepository.invoiceById(id)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    val orderId = existing.orderId
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invoice is not linked to an order")
                        )

                    val order = orderRepository.orderById(orderId)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))

                    if (order.items.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Order must contain at least one item to regenerate invoice")
                        )
                    }

                    val client = clientRepository.clientById(order.clientId)

                    val updated = existing.copy(
                        client = client?.let {
                            InvoiceClientSnapshot(
                                type = it.type,
                                name = it.displayName,
                                address = it.address,
                                phone = it.phone,
                                email = it.email
                            )
                        } ?: existing.client,
                        lines = buildLinesFromOrderItems(order.items),
                        subtotal = order.items.sumOf { it.price - it.taxAmount },
                        taxAmount = order.items.sumOf { it.taxAmount },
                        totalAmount = order.items.sumOf { it.price },
                        notes = order.notes
                    )

                    val saved = invoiceRepository.updateInvoice(id, updated)
                        ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    existing.filePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                    val filePath = invoiceGenerator.generateInvoicePdf(saved)
                    val withFile = invoiceRepository.updateInvoiceFilePath(saved.id, filePath) ?: saved

                    call.respond(HttpStatusCode.OK, withFile)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Invoice regeneration failed", "details" to (e.message ?: "unknown error"))
                    )
                }
            }

            get {
                call.respond(HttpStatusCode.OK, invoiceRepository.allInvoices())
            }

            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                val invoice = invoiceRepository.invoiceById(id)
                if (invoice == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))
                } else {
                    call.respond(HttpStatusCode.OK, invoice)
                }
            }

            get("/order/{orderId}") {
                val orderId = call.parameters["orderId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

                call.respond(HttpStatusCode.OK, invoiceRepository.invoicesByOrderId(orderId))
            }

            get("/number-format") {
                call.respond(
                    HttpStatusCode.OK,
                    InvoiceNumberFormatInfo(currentNumberTemplate(), nextInvoiceNumberFromSettings())
                )
            }

            put("/number-format") {
                val request = call.receive<InvoiceNumberFormatUpdateRequest>()
                val template = request.template.trim()
                if (template.isBlank()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Шаблон не може бути порожнім")
                    )
                }
                if (!template.endsWith("0")) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Шаблон має закінчуватися нулями — вони визначають кількість цифр номера, наприклад СФ-0000000")
                    )
                }
                appSettingsRepository.set(INVOICE_NUMBER_TEMPLATE_KEY, template)
                call.respond(
                    HttpStatusCode.OK,
                    InvoiceNumberFormatInfo(template, nextInvoiceNumberFromSettings())
                )
            }

            put("{id}/number") {
                try {
                    val id = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                    val existing = invoiceRepository.invoiceById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    val request = call.receive<InvoiceNumberOverrideRequest>()
                    val newNumber = request.number.trim()
                    if (newNumber.isBlank()) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Номер не може бути порожнім")
                        )
                    }
                    if (invoiceRepository.numberExists(newNumber, excludeId = id)) {
                        return@put call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to "Рахунок з номером $newNumber вже існує")
                        )
                    }

                    val renamed = invoiceRepository.updateInvoiceNumber(id, newNumber)
                        ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                    existing.filePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                    val filePath = invoiceGenerator.generateInvoicePdf(renamed)
                    val withFile = invoiceRepository.updateInvoiceFilePath(renamed.id, filePath) ?: renamed

                    call.respond(HttpStatusCode.OK, withFile)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Invoice number update failed", "details" to (e.message ?: "unknown error"))
                    )
                }
            }

            get("/download/{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                val invoice = invoiceRepository.invoiceById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))

                val filePath = invoice.filePath
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "PDF file not found"))

                val file = File(filePath)
                if (!file.exists()) {
                    return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "PDF file does not exist"))
                }

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter(ContentDisposition.Parameters.FileName, "${invoice.number}.pdf")
                        .toString()
                )
                call.respondFile(file)
            }

            delete("{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice ID"))

                invoiceRepository.invoiceById(id)?.filePath?.let { path ->
                    File(path).takeIf { file -> file.exists() }?.delete()
                }

                val deleted = invoiceRepository.deleteInvoice(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Invoice deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found"))
                }
            }
        }
    }
}

private fun buildLinesFromOrderItems(items: List<OrderItem>): List<InvoiceLine> =
    items.mapIndexed { index, item ->
        val lineTotal = item.price
        val unitPrice = if (item.quantity > 0) lineTotal / item.quantity else lineTotal
        InvoiceLine(
            lineNumber = index + 1,
            description = buildDescription(item),
            quantity = item.quantity,
            unit = item.unit.trimEnd('.'),
            usedMeters = item.usedMeters,
            unitPrice = unitPrice,
            lineTotal = lineTotal
        )
    }

private fun buildDescription(item: OrderItem): String {
    item.name?.takeIf { it.isNotBlank() }?.let { return it }

    val serviceText = when (item.serviceType) {
        ServiceType.DTF -> "DTF друк"
        ServiceType.UV_DTF -> "UV DTF друк"
        ServiceType.DTF_TRANSFER_ONLY -> "DTF трансфер"
        ServiceType.DESIGN_ONLY -> "Підготовка дизайну"
    }

    val productText = when (item.productType) {
        ProductType.T_SHIRT -> "футболка"
        ProductType.HOODIE -> "худі"
        ProductType.SWEATSHIRT -> "світшот"
        ProductType.SHOPPER_BAG -> "шопер"
        ProductType.CAP -> "кепка"
        ProductType.APRON -> "фартух"
        ProductType.BACKPACK -> "рюкзак"
        ProductType.UNIFORM -> "уніформа"
        ProductType.OTHER_TEXTILE -> "інший текстиль"
        ProductType.MUG -> "чашка"
        ProductType.THERMOS -> "термос"
        ProductType.BOTTLE -> "пляшка"
        ProductType.PHONE_CASE -> "чохол"
        ProductType.KEYCHAIN -> "брелок"
        ProductType.PEN -> "ручка"
        ProductType.NOTEBOOK -> "блокнот"
        ProductType.SIGN -> "вивіска"
        ProductType.GIFT_BOX -> "подарункова коробка"
        ProductType.OTHER_HARD -> "інший виріб (тверде)"
        ProductType.OTHER -> "інший виріб"
    }

    return "$serviceText на $productText"
}