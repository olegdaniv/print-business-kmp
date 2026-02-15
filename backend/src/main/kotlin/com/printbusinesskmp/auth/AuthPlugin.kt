package com.printbusinesskmp.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

fun Application.configureJwtAuthentication(
    appJwtService: AppJwtService
) {
    install(Authentication) {
        jwt("app-jwt") {
            realm = "print-business-api"
            verifier(appJwtService.verifier)
            validate { credential ->
                appJwtService.toPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respondApiError(
                    status = HttpStatusCode.Unauthorized,
                    error = "unauthorized",
                    message = "Missing or invalid access token"
                )
            }
        }
    }
}
