package com.printbusinesskmp.routes

import com.printbusinesskmp.models.PartnerCreateRequest
import com.printbusinesskmp.models.PartnerUpdateRequest
import com.printbusinesskmp.repository.PartnerRepository
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

private val partnerRepository = PartnerRepository()

fun Route.configurePartnerRoutes() {
    authenticate("app-jwt") {
        route("/api/partners") {
            get {
                call.respond(HttpStatusCode.OK, partnerRepository.allPartners())
            }

            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing partner ID"))

                val partner = partnerRepository.partnerById(id)
                if (partner == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Partner not found"))
                } else {
                    call.respond(HttpStatusCode.OK, partner)
                }
            }

            post {
                try {
                    val request = call.receive<PartnerCreateRequest>()
                    validatePartnerName(request.name)
                    call.respond(HttpStatusCode.Created, partnerRepository.addPartner(request))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            put("{id}") {
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing partner ID"))

                try {
                    val request = call.receive<PartnerUpdateRequest>()
                    validatePartnerName(request.name)
                    val updated = partnerRepository.updatePartner(id, request)
                    if (updated == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Partner not found"))
                    } else {
                        call.respond(HttpStatusCode.OK, updated)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            delete("{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing partner ID"))

                val deleted = partnerRepository.deletePartner(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Partner deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Partner not found"))
                }
            }
        }
    }
}

private fun validatePartnerName(name: String) {
    if (name.isBlank()) {
        throw IllegalArgumentException("Partner name is required")
    }
}
