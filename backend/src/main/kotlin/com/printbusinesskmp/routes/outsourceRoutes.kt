package com.printbusinesskmp.routes

import com.printbusinesskmp.models.OutsourceJobCreateRequest
import com.printbusinesskmp.models.OutsourceJobUpdateRequest
import com.printbusinesskmp.repository.OutsourceJobRepository
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

private val outsourceJobRepository = OutsourceJobRepository()

fun Route.configureOutsourceRoutes() {
    authenticate("app-jwt") {
        route("/api/outsource-jobs") {
            get {
                val orderId = call.request.queryParameters["orderId"]
                if (orderId == null) {
                    call.respond(HttpStatusCode.OK, outsourceJobRepository.allJobs())
                } else {
                    call.respond(HttpStatusCode.OK, outsourceJobRepository.jobsByOrderId(orderId))
                }
            }

            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing outsource job ID"))

                val job = outsourceJobRepository.jobById(id)
                if (job == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Outsource job not found"))
                } else {
                    call.respond(HttpStatusCode.OK, job)
                }
            }

            post {
                try {
                    val request = call.receive<OutsourceJobCreateRequest>()
                    validateOutsourceJob(request.costToYou, request.description)
                    call.respond(HttpStatusCode.Created, outsourceJobRepository.addJob(request))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            put("{id}") {
                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing outsource job ID"))

                try {
                    val request = call.receive<OutsourceJobUpdateRequest>()
                    validateOutsourceJob(request.costToYou, request.description)
                    val updated = outsourceJobRepository.updateJob(id, request)
                    if (updated == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Outsource job not found"))
                    } else {
                        call.respond(HttpStatusCode.OK, updated)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
                }
            }

            delete("{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing outsource job ID"))

                val deleted = outsourceJobRepository.deleteJob(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Outsource job deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Outsource job not found"))
                }
            }
        }
    }
}

private fun validateOutsourceJob(costToYou: Double, description: String) {
    if (description.isBlank()) {
        throw IllegalArgumentException("Description is required")
    }
    if (costToYou < 0.0) {
        throw IllegalArgumentException("Cost cannot be negative")
    }
}
