package com.printbusinesskmp.routes

import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.InvoiceClientSnapshot
import com.printbusinesskmp.models.InvoiceLine
import com.printbusinesskmp.models.InvoiceSellerSnapshot
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.repository.BusinessProfileRepository
import com.printbusinesskmp.repository.ClientRepository
import com.printbusinesskmp.repository.InvoiceRepository
import com.printbusinesskmp.repository.OrderRepository
import com.printbusinesskmp.services.InvoiceGenerator
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.io.File
import java.util.UUID
import kotlin.time.Clock

private val invoiceRepository = InvoiceRepository()
private val orderRepository = OrderRepository()
private val clientRepository = ClientRepository()
private val businessProfileRepository = BusinessProfileRepository()
private val invoiceGenerator = InvoiceGenerator()

fun Route.configureInvoiceRoutes() {
    authenticate("app-jwt") {
        route("/api/invoices") {
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

                    val number = invoiceRepository.nextInvoiceNumber()
                    val now = Clock.System.now()

                    val lines = order.items.mapIndexed { index, item ->
                        val lineTotal = item.price
                        val unitPrice = if (item.quantity > 0) {
                            lineTotal / item.quantity
                        } else {
                            lineTotal
                        }

                        InvoiceLine(
                            lineNumber = index + 1,
                            description = buildDescription(item.serviceType, item.productType, item.usedMeters),
                            quantity = item.quantity,
                            usedMeters = item.usedMeters,
                            unitPrice = unitPrice,
                            lineTotal = lineTotal
                        )
                    }

                    val subtotal = order.items.sumOf { item -> item.price - item.taxAmount }
                    val taxAmount = order.items.sumOf { item -> item.taxAmount }
                    val totalAmount = order.items.sumOf { item -> item.price }

                    val invoice = Invoice(
                        id = UUID.randomUUID().toString(),
                        number = number,
                        orderId = order.id,
                        issuedAt = now,
                        seller = InvoiceSellerSnapshot(
                            ownerName = profile.ownerName,
                            taxId = profile.taxId,
                            address = profile.address,
                            iban = profile.iban,
                            bankName = profile.bankName,
                            taxPercent = profile.taxPercent
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

private fun buildDescription(serviceType: ServiceType, productType: ProductType, usedMeters: Double): String {
    val serviceText = when (serviceType) {
        ServiceType.DTF -> "DTF друк"
        ServiceType.UV_DTF -> "UV DTF друк"
    }

    val productText = when (productType) {
        ProductType.T_SHIRT -> "футболка"
        ProductType.HOODIE -> "худі"
        ProductType.OTHER -> "інший виріб"
    }

    return "$serviceText на $productText (${String.format("%.2f", usedMeters)} м)"
}
