package com.printbusinesskmp.routes

import com.printbusinesskmp.auth.AllowlistService
import com.printbusinesskmp.auth.AppJwtService
import com.printbusinesskmp.auth.AuthException
import com.printbusinesskmp.auth.GoogleAuthRequest
import com.printbusinesskmp.auth.GoogleAuthResponse
import com.printbusinesskmp.auth.GoogleIdTokenVerifier
import com.printbusinesskmp.auth.respondApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.configureAuthRoutes(
    googleIdTokenVerifier: GoogleIdTokenVerifier,
    allowlistService: AllowlistService,
    appJwtService: AppJwtService
) {
    route("/auth") {
        post("/google") {
            val request = try {
                call.receive<GoogleAuthRequest>()
            } catch (_: BadRequestException) {
                return@post call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    error = "invalid_request",
                    message = "Request body must be valid JSON"
                )
            }

            if (request.idToken.isBlank()) {
                return@post call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    error = "invalid_request",
                    message = "idToken must not be blank"
                )
            }

            try {
                val verifiedGoogleClaims = googleIdTokenVerifier.verify(request.idToken)

                if (!allowlistService.isAllowed(verifiedGoogleClaims.email)) {
                    return@post call.respondApiError(
                        status = HttpStatusCode.Forbidden,
                        error = "forbidden",
                        message = "Email '${verifiedGoogleClaims.email}' is not in the allowlist"
                    )
                }

                val accessToken = appJwtService.issueAccessToken(
                    userId = verifiedGoogleClaims.sub,
                    email = verifiedGoogleClaims.email,
                    name = verifiedGoogleClaims.name
                )

                call.respond(
                    HttpStatusCode.OK,
                    GoogleAuthResponse(
                        accessToken = accessToken,
                        expiresInSeconds = appJwtService.expiresInSeconds,
                        email = verifiedGoogleClaims.email,
                        name = verifiedGoogleClaims.name
                    )
                )
            } catch (error: AuthException) {
                call.respondApiError(
                    status = error.status,
                    error = error.error,
                    message = error.message
                )
            }
        }
    }
}
