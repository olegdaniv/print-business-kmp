package com.printbusinesskmp.routes

import com.printbusinesskmp.models.PaymentUpsertRequest
import com.printbusinesskmp.repository.PaymentRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val paymentRepository = PaymentRepository()

fun Route.configurePaymentRoutes() {
    authenticate("app-jwt") {
        route("/api/payments") {
            get {
                val orderId = call.request.queryParameters["orderId"]?.takeIf { it.isNotBlank() }
                call.respond(HttpStatusCode.OK, paymentRepository.allPayments(orderId))
            }

            get("/legacy-partial") {
                call.respond(HttpStatusCode.OK, paymentRepository.legacyPartialOrderIds())
            }

            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing payment ID"))

                val payment = paymentRepository.paymentById(id)
                if (payment == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment not found"))
                } else {
                    call.respond(HttpStatusCode.OK, payment)
                }
            }

            post {
                try {
                    val request = call.receive<PaymentUpsertRequest>()
                    call.respond(HttpStatusCode.Created, paymentRepository.addPayment(request))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            put("{id}") {
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing payment ID"))

                try {
                    val request = call.receive<PaymentUpsertRequest>()
                    val updated = paymentRepository.updatePayment(id, request)
                    if (updated == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment not found"))
                    } else {
                        call.respond(HttpStatusCode.OK, updated)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            delete("{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing payment ID"))

                if (paymentRepository.deletePayment(id)) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Payment deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Payment not found"))
                }
            }
        }
    }
}
