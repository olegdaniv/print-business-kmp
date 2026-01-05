package com.printbusinesskmp.routes

import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.repository.ClientRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val clientRepository = ClientRepository()

fun Route.configureClientRoutes() {
    route("/api/clients") {
        // GET /api/clients - List all clients
        get {
            try {
                val clients = clientRepository.allClients()
                call.respond(HttpStatusCode.OK, clients)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // GET /api/clients/{id} - Get client by id
        get("{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing client ID")
                )

                val client = clientRepository.clientById(id)
                if (client != null) {
                    call.respond(HttpStatusCode.OK, client)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Client not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // POST /api/clients - Create new client
        post {
            try {
                val request = call.receive<ClientCreateRequest>()
                val created = clientRepository.addClient(request)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"), "type" to e.javaClass.simpleName)
                )
            }
        }

        // PUT /api/clients/{id} - Update client
        put("{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing client ID")
                )

                val request = call.receive<ClientUpdateRequest>()
                val updated = clientRepository.updateClient(id, request)

                if (updated != null) {
                    call.respond(HttpStatusCode.OK, updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Client not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }

        // DELETE /api/clients/{id} - Delete client
        delete("{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing client ID")
                )

                val deleted = clientRepository.deleteClient(id)
                if (deleted) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Client deleted successfully")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Client not found")
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