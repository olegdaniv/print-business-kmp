package com.printbusinesskmp.routes

import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.repository.ClientRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val clientRepository = ClientRepository()

fun Route.configureClientRoutes() {
    route("/api/clients") {
        get {
            call.respond(HttpStatusCode.OK, clientRepository.allClients())
        }

        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing client ID"))

            val client = clientRepository.clientById(id)
            if (client == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
            } else {
                call.respond(HttpStatusCode.OK, client)
            }
        }

        post {
            try {
                val request = call.receive<ClientCreateRequest>()
                validateClient(request.displayName, request.phone, request.address)
                val created = clientRepository.addClient(request)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        put("{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing client ID"))

            try {
                val request = call.receive<ClientUpdateRequest>()
                validateClient(request.displayName, request.phone, request.address)

                val updated = clientRepository.updateClient(id, request)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing client ID"))

            try {
                val deleted = clientRepository.deleteClient(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Client deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                }
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "Delete conflict")))
            }
        }
    }
}

private fun validateClient(displayName: String, phone: String, address: String) {
    if (displayName.isBlank()) {
        throw IllegalArgumentException("Client name is required")
    }
    if (phone.isBlank()) {
        throw IllegalArgumentException("Client phone is required")
    }
    if (address.isBlank()) {
        throw IllegalArgumentException("Client address is required")
    }
}
