package com.printbusinesskmp.routes

import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.repository.OrderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

private val orderRepository = OrderRepository()

@Serializable
data class UpdateStatusRequest(val status: String)

fun Route.configureOrderRoutes() {
    route("/api/orders") {
        // GET /api/orders - List all orders
        get {
            try {
                val orders = orderRepository.allOrders()
                call.respond(HttpStatusCode.OK, orders)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/orders/{id} - Get order by id with items
        get("{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                val order = orderRepository.orderById(id)
                if (order != null) {
                    call.respond(HttpStatusCode.OK, order)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // POST /api/orders - Create new order
        post {
            try {
                val order = call.receive<Order>()
                val created = orderRepository.addOrder(order)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // PUT /api/orders/{id} - Update order
        put("{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                val order = call.receive<Order>()
                val updated = orderRepository.updateOrder(id, order)

                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // PUT /api/orders/{id}/status - Update order status
        put("{id}/status") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                val request = call.receive<UpdateStatusRequest>()
                val status = try {
                    OrderStatus.valueOf(request.status)
                } catch (e: IllegalArgumentException) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid status value")
                    )
                }

                val updated = orderRepository.updateOrderStatus(id, status)

                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // DELETE /api/orders/{id} - Delete order
        delete("{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing order ID")
                )

                val deleted = orderRepository.deleteOrder(id)
                if (deleted) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Order deleted successfully")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
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