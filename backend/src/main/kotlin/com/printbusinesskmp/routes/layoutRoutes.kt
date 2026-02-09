package com.printbusinesskmp.routes

import com.printbusinesskmp.models.LayoutCreateRequest
import com.printbusinesskmp.models.LayoutStatus
import com.printbusinesskmp.models.LayoutUpdateRequest
import com.printbusinesskmp.repository.LayoutRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

private val layoutRepository = LayoutRepository()

fun Route.configureLayoutRoutes() {
    route("/api/layouts") {
        get {
            val search = call.request.queryParameters["search"]
            val clientId = call.request.queryParameters["clientId"]
            val statusRaw = call.request.queryParameters["status"]

            val status = if (statusRaw.isNullOrBlank()) {
                null
            } else {
                runCatching { LayoutStatus.valueOf(statusRaw.trim().uppercase()) }
                    .getOrElse {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid layout status filter")
                        )
                    }
            }

            call.respond(
                HttpStatusCode.OK,
                layoutRepository.allLayouts(
                    search = search,
                    clientId = clientId,
                    status = status
                )
            )
        }

        get("{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing layout ID"))

            val layout = layoutRepository.layoutById(id)
            if (layout == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Layout not found"))
            } else {
                call.respond(HttpStatusCode.OK, layout)
            }
        }

        post {
            try {
                val request = call.receive<LayoutCreateRequest>()
                call.respond(HttpStatusCode.Created, layoutRepository.addLayout(request))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        put("{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing layout ID"))

            try {
                val request = call.receive<LayoutUpdateRequest>()
                val updated = layoutRepository.updateLayout(id, request)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Layout not found"))
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Validation error")))
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing layout ID"))

            val deleted = layoutRepository.deleteLayout(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Layout deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Layout not found"))
            }
        }
    }
}
