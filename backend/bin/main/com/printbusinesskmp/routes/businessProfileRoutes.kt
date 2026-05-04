package com.printbusinesskmp.routes

import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import com.printbusinesskmp.repository.BusinessProfileRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val businessProfileRepository = BusinessProfileRepository()

fun Route.configureBusinessProfileRoutes() {
    authenticate("app-jwt") {
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
}

private fun validateBusinessProfile(request: BusinessProfileUpsertRequest) {
    if (request.ownerName.isBlank()) throw IllegalArgumentException("Вкажіть ПІБ")

    val edrpou = request.edrpou.filter { it.isDigit() }
    if (edrpou.length != 8) throw IllegalArgumentException("ЄДРПОУ має містити рівно 8 цифр")

    val rawIpn = request.ipn
    if (!rawIpn.isNullOrBlank()) {
        val ipnDigits = rawIpn.filter { it.isDigit() }
        if (ipnDigits.length != 10) throw IllegalArgumentException("ІПН має містити рівно 10 цифр")
    }

    val iban = request.iban.replace(" ", "").uppercase()
    if (!iban.startsWith("UA") || iban.length != 29)
        throw IllegalArgumentException("IBAN має бути у форматі UA + 27 символів (29 символів разом)")

    val rawMfo = request.mfo
    if (!rawMfo.isNullOrBlank()) {
        val mfoDigits = rawMfo.filter { it.isDigit() }
        if (mfoDigits.length != 6) throw IllegalArgumentException("МФО має містити рівно 6 цифр")
    }

    val phone = request.phone?.filter { it.isDigit() }
    if (phone.isNullOrEmpty() || phone.length != 10)
        throw IllegalArgumentException("Телефон має містити рівно 10 цифр (формат 0XXXXXXXXX)")

    if (request.address.isBlank()) throw IllegalArgumentException("Вкажіть адресу")
}
