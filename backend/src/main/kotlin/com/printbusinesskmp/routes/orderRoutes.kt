package com.printbusinesskmp.routes

import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.repository.OrderRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

private val orderRepository = OrderRepository()

@Serializable
data class UpdateOrderStateRequest(
    val status: OrderStatus? = null,
    val paymentStatus: PaymentStatus? = null
)

fun Route.configureOrderRoutes() {
    route("/api/orders") {
        get {
            call.respond(HttpStatusCode.OK, orderRepository.allOrders())
        }

        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

            val order = orderRepository.orderById(id)
            if (order == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            } else {
                call.respond(HttpStatusCode.OK, order)
            }
        }

        post {
            try {
                val request = call.receive<OrderCreateRequest>()
                val created = orderRepository.addOrder(request)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        put("{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

            try {
                val request = call.receive<OrderUpdateRequest>()
                val updated = orderRepository.updateOrder(id, request)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        patch("{id}/state") {
            val id = call.parameters["id"]
                ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

            val request = call.receive<UpdateOrderStateRequest>()
            val targetStatus = request.status
                ?: orderRepository.orderById(id)?.status
                ?: return@patch call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))

            val updated = orderRepository.updateStatus(
                id = id,
                status = targetStatus,
                paymentStatus = request.paymentStatus
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing order ID"))

            val deleted = orderRepository.deleteOrder(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Order deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            }
        }
    }
}
