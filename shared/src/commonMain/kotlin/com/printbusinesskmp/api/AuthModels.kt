package com.printbusinesskmp.api

import io.ktor.http.HttpStatusCode

data class AuthSession(
    val accessToken: String,
    val expiresInSeconds: Long,
    val email: String,
    val name: String? = null
)

class NotAllowlistedException(message: String) : RuntimeException(message)

class SessionExpiredException(message: String = "Session expired. Please sign in again.") : RuntimeException(message)

class AuthRequestException(
    val status: HttpStatusCode,
    message: String
) : RuntimeException(message)
