package com.printbusinesskmp.auth

import java.time.Clock
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppJwtServiceTest {

    @Test
    fun `issues token that verifier accepts and exposes expected claims`() {
        val jwtService = AppJwtService(
            config = AppJwtConfig(
                secret = "test-secret-should-be-long-enough",
                issuer = "print-business-tests",
                audience = "print-business-client"
            ),
            clock = Clock.systemUTC()
        )

        val token = jwtService.issueAccessToken(
            userId = "google-sub-123",
            email = "admin@example.com",
            name = "Admin User"
        )

        val decoded = jwtService.verifier.verify(token)
        assertEquals("google-sub-123", decoded.subject)
        assertEquals("google-sub-123", decoded.getClaim("userId").asString())
        assertEquals("admin@example.com", decoded.getClaim("email").asString())
        assertEquals("Admin User", decoded.getClaim("name").asString())
        assertEquals(listOf("admin"), decoded.getClaim("roles").asList(String::class.java))

        val ttlSeconds = Duration.between(decoded.issuedAt.toInstant(), decoded.expiresAt.toInstant()).seconds
        assertEquals(43_200L, ttlSeconds)

        val principal = jwtService.toPrincipal(decoded)
        assertNotNull(principal)
        assertEquals("google-sub-123", principal.userId)
        assertEquals("admin@example.com", principal.email)
    }
}
