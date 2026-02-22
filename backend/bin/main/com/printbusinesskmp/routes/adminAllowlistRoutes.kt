package com.printbusinesskmp.routes

import com.printbusinesskmp.auth.AllowlistService
import com.printbusinesskmp.auth.AllowedEmailEntry
import com.printbusinesskmp.auth.ApiErrorResponse
import com.printbusinesskmp.auth.AppPrincipal
import com.printbusinesskmp.auth.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Serializable
private data class AddAllowedEmailRequest(
    val email: String,
    val note: String? = null
)

@Serializable
private data class AllowedEmailResponse(
    val email: String,
    val createdAt: String,
    val note: String? = null
)

@Serializable
private data class AllowedEmailListResponse(
    val items: List<AllowedEmailResponse>
)

@Serializable
private data class AllowedEmailDeleteResponse(
    val email: String,
    val removed: Boolean
)

fun Route.configureAllowlistAdminRoutes(
    allowlistService: AllowlistService
) {
    authenticate("app-jwt") {
        route("/admin/allowed-emails") {
            get {
                if (!call.requireAdminPrincipal()) return@get

                val emails = try {
                    allowlistService.listEmails()
                } catch (error: Exception) {
                    return@get call.respondApiError(
                        status = HttpStatusCode.ServiceUnavailable,
                        error = "allowlist_unavailable",
                        message = "Allowlist database is unavailable"
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    AllowedEmailListResponse(items = emails.map { it.toResponse() })
                )
            }

            post {
                if (!call.requireAdminPrincipal()) return@post

                val request = try {
                    call.receive<AddAllowedEmailRequest>()
                } catch (_: BadRequestException) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(
                            error = "invalid_request",
                            message = "Request body must be valid JSON"
                        )
                    )
                }

                val created = try {
                    allowlistService.addEmail(request.email, request.note)
                } catch (error: IllegalArgumentException) {
                    return@post call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        error = "invalid_email",
                        message = error.message ?: "Email is invalid"
                    )
                } catch (error: IllegalStateException) {
                    return@post call.respondApiError(
                        status = HttpStatusCode.Conflict,
                        error = "email_exists",
                        message = error.message ?: "Email already exists"
                    )
                } catch (error: Exception) {
                    return@post call.respondApiError(
                        status = HttpStatusCode.ServiceUnavailable,
                        error = "allowlist_unavailable",
                        message = "Allowlist database is unavailable"
                    )
                }

                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            delete("{email}") {
                if (!call.requireAdminPrincipal()) return@delete

                val rawEmail = call.parameters["email"]
                    ?: return@delete call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        error = "invalid_request",
                        message = "Missing email path parameter"
                    )
                val decodedEmail = URLDecoder.decode(rawEmail, StandardCharsets.UTF_8)

                val removed = try {
                    allowlistService.removeEmail(decodedEmail)
                } catch (error: IllegalArgumentException) {
                    return@delete call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        error = "invalid_email",
                        message = error.message ?: "Email is invalid"
                    )
                } catch (error: Exception) {
                    return@delete call.respondApiError(
                        status = HttpStatusCode.ServiceUnavailable,
                        error = "allowlist_unavailable",
                        message = "Allowlist database is unavailable"
                    )
                }

                if (!removed) {
                    return@delete call.respondApiError(
                        status = HttpStatusCode.NotFound,
                        error = "not_found",
                        message = "Email not found in allowlist"
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    AllowedEmailDeleteResponse(
                        email = decodedEmail.trim().lowercase(),
                        removed = true
                    )
                )
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireAdminPrincipal(): Boolean {
    val principal = principal<AppPrincipal>()
    if (principal == null) {
        respondApiError(
            status = HttpStatusCode.Unauthorized,
            error = "unauthorized",
            message = "Missing or invalid access token"
        )
        return false
    }

    if (!principal.roles.any { it.equals("admin", ignoreCase = true) }) {
        respondApiError(
            status = HttpStatusCode.Forbidden,
            error = "forbidden",
            message = "Admin role is required"
        )
        return false
    }

    return true
}

private fun AllowedEmailEntry.toResponse(): AllowedEmailResponse {
    return AllowedEmailResponse(
        email = email,
        createdAt = createdAt.toString(),
        note = note
    )
}
