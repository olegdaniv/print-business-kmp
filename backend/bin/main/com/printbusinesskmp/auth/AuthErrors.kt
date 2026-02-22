package com.printbusinesskmp.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class AuthException(
    val status: HttpStatusCode,
    val error: String,
    override val message: String
) : RuntimeException(message)

suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    error: String,
    message: String
) {
    respond(status, ApiErrorResponse(error = error, message = message))
}
