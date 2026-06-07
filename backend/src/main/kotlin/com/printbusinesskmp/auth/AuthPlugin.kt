package com.printbusinesskmp.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
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

/**
 * Local-only authentication used by the embedded desktop server.
 *
 * Registers a provider under the same name ("app-jwt") that every route already
 * uses, but it always authenticates a fixed local principal — no JWT, no Google,
 * no token required. Safe because no business route reads the principal.
 */
fun Application.configureLocalAuthentication() {
    install(Authentication) {
        register(LocalAuthenticationProvider(LocalAuthenticationProvider.Config("app-jwt")))
    }
}

private val LOCAL_PRINCIPAL = AppPrincipal(
    userId = "local",
    email = "local@printbusiness.app",
    name = "Local user",
    roles = listOf("admin")
)

private class LocalAuthenticationProvider(
    config: Config
) : AuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal("app-jwt", LOCAL_PRINCIPAL)
    }

    class Config(name: String?) : AuthenticationProvider.Config(name)
}
