package com.printbusinesskmp.auth

import io.ktor.server.auth.Principal
import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class GoogleAuthResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val email: String,
    val name: String? = null
)

@Serializable
data class GoogleClientIdResponse(
    val clientId: String
)

@Serializable
data class ApiErrorResponse(
    val error: String,
    val message: String
)

data class GoogleUserClaims(
    val sub: String,
    val email: String,
    val name: String? = null,
    val picture: String? = null
)

data class AppPrincipal(
    val userId: String,
    val email: String,
    val name: String? = null,
    val roles: List<String> = emptyList()
) : Principal
