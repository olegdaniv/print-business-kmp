package com.printbusinesskmp.routes

import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import com.printbusinesskmp.repository.BusinessProfileRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val businessProfileRepository = BusinessProfileRepository()

fun Route.configureBusinessProfileRoutes() {
    route("/api/business-profile") {
        get {
            val profile = businessProfileRepository.getProfile()
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Business profile not configured"))
            } else {
                call.respond(HttpStatusCode.OK, profile)
            }
        }

        put {
            try {
                val request = call.receive<BusinessProfileUpsertRequest>()
                validateBusinessProfile(request)
                val profile = businessProfileRepository.upsert(request)
                call.respond(HttpStatusCode.OK, profile)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }
    }
}

private fun validateBusinessProfile(request: BusinessProfileUpsertRequest) {
    if (request.ownerName.isBlank()) throw IllegalArgumentException("Owner name is required")
    if (request.taxId.isBlank()) throw IllegalArgumentException("Tax ID is required")
    if (request.address.isBlank()) throw IllegalArgumentException("Address is required")
    if (request.iban.isBlank()) throw IllegalArgumentException("IBAN is required")
    if (request.bankName.isBlank()) throw IllegalArgumentException("Bank name is required")
    if (request.taxPercent < 0.0) throw IllegalArgumentException("Tax percent cannot be negative")
}
