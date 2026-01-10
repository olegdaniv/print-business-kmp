package com.printbusinesskmp.routes

import com.printbusinesskmp.models.ApiError
import com.printbusinesskmp.models.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val username: String
)

/**
 * Authentication routes demonstrating the Keys-First error handling architecture.
 * The backend returns stable error codes instead of user-facing messages.
 */
fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                // Simulate authentication failure for demo purposes
                if (request.username != "admin" || request.password != "password") {
                    val error = ApiError(
                        errorCode = ApiError.INVALID_CREDENTIALS,
                        details = mapOf("username" to request.username),
                        timestamp = Clock.System.now().toString()
                    )

                    call.respond(
                        status = HttpStatusCode.Unauthorized,
                        message = ApiResponse.Error(error)
                    )
                    return@post
                }

                // Successful login
                val response = LoginResponse(
                    token = "sample-jwt-token-${Clock.System.now().toEpochMilliseconds()}",
                    username = request.username
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = ApiResponse.Success(response)
                )

            } catch (e: Exception) {
                val error = ApiError(
                    errorCode = ApiError.SERVER_ERROR,
                    details = mapOf("message" to (e.message ?: "Unknown error")),
                    timestamp = Clock.System.now().toString()
                )

                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ApiResponse.Error(error)
                )
            }
        }

        // Demo endpoint that always returns USER_NOT_FOUND error
        get("/user/{id}") {
            val userId = call.parameters["id"]
            val error = ApiError(
                errorCode = ApiError.USER_NOT_FOUND,
                details = mapOf("userId" to (userId ?: "unknown")),
                timestamp = Clock.System.now().toString()
            )

            call.respond(
                status = HttpStatusCode.NotFound,
                message = ApiResponse.Error(error)
            )
        }
    }
}