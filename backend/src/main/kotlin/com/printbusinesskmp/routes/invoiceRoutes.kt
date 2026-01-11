package com.printbusinesskmp.routes

import com.printbusinesskmp.models.*
import com.printbusinesskmp.repository.ClientRepository
import com.printbusinesskmp.repository.InvoiceRepository
import com.printbusinesskmp.repository.OrderRepository
import com.printbusinesskmp.services.InvoiceGenerator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

private val invoiceRepository = InvoiceRepository()
private val orderRepository = OrderRepository()
private val clientRepository = ClientRepository()
private val invoiceGenerator = InvoiceGenerator()
private val fopDetails = FopDetails()

fun Route.configureInvoiceRoutes() {
    route("/api/invoices") {

        // POST /api/invoices/generate/{orderId} - Generate invoice for an order
        post("/generate/{orderId}") {
            try {
                val orderId = call.parameters["orderId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                // Fetch order with items
                val order = orderRepository.orderById(orderId)
                if (order == null) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
                    )
                }

                // Fetch client details
                val client = clientRepository.clientById(order.clientId)
                if (client == null) {
                    return@post call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Client not found")
                    )
                }

                // Get next invoice number
                val invoiceNumber = invoiceRepository.getNextInvoiceNumber()

                // Create invoice client object
                val invoiceClient = InvoiceClient(
                    name = client.name,
                    phone = client.phone,
                    email = client.email,
                    address = null // Could be added to client model if needed
                )

                // Create invoice items from order items
                val invoiceItems = order.items.mapIndexed { index, orderItem ->
                    val description = buildItemDescription(orderItem)
                    InvoiceItem(
                        number = index + 1,
                        description = description,
                        quantity = orderItem.quantity,
                        unit = "шт.",
                        pricePerUnit = orderItem.sellingPrice / orderItem.quantity,
                        totalPrice = orderItem.sellingPrice
                    )
                }

                // Create invoice object (without filePath initially)
                val invoice = Invoice(
                    id = UUID.randomUUID().toString(),
                    number = invoiceNumber,
                    date = kotlin.time.Clock.System.now(),
                    orderId = orderId,
                    client = invoiceClient,
                    items = invoiceItems,
                    totalAmount = order.totalPrice,
                    notes = order.notes,
                    generatedAt = kotlin.time.Clock.System.now(),
                    filePath = null
                )

                // Save invoice to database first
                val savedInvoice = invoiceRepository.addInvoice(invoice)

                // Generate PDF
                val pdfFilePath = invoiceGenerator.generateInvoicePdf(savedInvoice, fopDetails)

                // Update invoice with file path
                val updatedInvoice = invoiceRepository.updateInvoiceFilePath(savedInvoice.id, pdfFilePath)

                // Update order to mark invoice as generated
                orderRepository.updateOrder(orderId, order.copy(invoiceGenerated = true))

                call.respond(HttpStatusCode.Created, updatedInvoice ?: savedInvoice)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/invoices/{id} - Get invoice by ID
        get("{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing invoice ID")
                )

                val invoice = invoiceRepository.invoiceById(id)
                if (invoice != null) {
                    call.respond(HttpStatusCode.OK, invoice)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Invoice not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/invoices/order/{orderId} - Get invoices for specific order
        get("/order/{orderId}") {
            try {
                val orderId = call.parameters["orderId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                val invoices = invoiceRepository.invoicesByOrderId(orderId)
                call.respond(HttpStatusCode.OK, invoices)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/invoices/download/{id} - Download PDF file
        get("/download/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing invoice ID")
                )

                val invoice = invoiceRepository.invoiceById(id)
                if (invoice == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Invoice not found")
                    )
                }

                if (invoice.filePath == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Invoice PDF file not found")
                    )
                }

                val file = File(invoice.filePath)
                if (!file.exists()) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Invoice PDF file does not exist")
                    )
                }

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "invoice_${invoice.number}.pdf"
                    ).toString()
                )
                call.respondFile(file)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/invoices - List all invoices
        get {
            try {
                val invoices = invoiceRepository.allInvoices()
                call.respond(HttpStatusCode.OK, invoices)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // DELETE /api/invoices/{id} - Delete invoice
        delete("{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing invoice ID")
                )

                // Get invoice to delete PDF file
                val invoice = invoiceRepository.invoiceById(id)
                if (invoice?.filePath != null) {
                    val file = File(invoice.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                val deleted = invoiceRepository.deleteInvoice(id)
                if (deleted) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Invoice deleted successfully")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Invoice not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
    }
}

private fun buildItemDescription(orderItem: OrderItem): String {
    // Build a human-readable description in Ukrainian
    val productType = when (orderItem.productType) {
        ProductType.T_SHIRT -> "футболка"
        ProductType.HOODIE -> "худі"
        ProductType.CAP -> "кепка"
        ProductType.BAG -> "сумка"
        ProductType.CUSTOM -> "кастомний виріб"
    }

    val printArea = when (orderItem.printArea) {
        PrintArea.FRONT -> "друк спереду"
        PrintArea.BACK -> "друк на спині"
        PrintArea.BOTH -> "друк з обох сторін"
        PrintArea.SLEEVE -> "друк на рукаві"
        PrintArea.CUSTOM -> "кастомний друк"
    }

    return "Термодрук на виробі: $productType (${orderItem.color}, розмір ${orderItem.size}, $printArea)"
}
