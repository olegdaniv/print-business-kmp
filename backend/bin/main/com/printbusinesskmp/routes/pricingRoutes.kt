package com.printbusinesskmp.routes

import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.utils.PricingCalculator
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.configurePricingRoutes() {
    authenticate("app-jwt") {
        route("/api/pricing") {
            post("/calculate") {
                try {
                    val request = call.receive<PricingRequest>()
                    val result = PricingCalculator.calculate(request)
                    call.respond(HttpStatusCode.OK, result)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }
        }
    }
}
