package com.printbusinesskmp.routes

import com.printbusinesskmp.models.SavedItemBulkUpsertRequest
import com.printbusinesskmp.models.SavedItemCreateRequest
import com.printbusinesskmp.repository.SavedItemRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val savedItemRepository = SavedItemRepository()

fun Route.configureSavedItemRoutes() {
    authenticate("app-jwt") {
        route("/api/saved-items") {
            get {
                call.respond(HttpStatusCode.OK, savedItemRepository.all())
            }

            post {
                val request = call.receive<SavedItemCreateRequest>()
                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Назва не може бути порожньою"))
                    return@post
                }
                val created = savedItemRepository.add(request)
                call.respond(HttpStatusCode.Created, created)
            }

            post("bulk-upsert") {
                val request = call.receive<SavedItemBulkUpsertRequest>()
                val results = request.items
                    .filter { it.name.isNotBlank() }
                    .map { savedItemRepository.upsertByName(it) }
                call.respond(HttpStatusCode.OK, results)
            }

            delete("{id}") {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val deleted = savedItemRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
                }
            }
        }
    }
}
