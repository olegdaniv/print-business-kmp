package com.printbusinesskmp.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import java.time.Clock
import java.time.Instant
import java.util.Date

data class AppJwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresInSeconds: Long = 43_200L
)

class AppJwtService(
    private val config: AppJwtConfig,
    private val clock: Clock = Clock.systemUTC()
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    val expiresInSeconds: Long = config.expiresInSeconds

    fun issueAccessToken(
        userId: String,
        email: String,
        name: String?
    ): String {
        val now = Instant.now(clock)
        val expiresAt = now.plusSeconds(config.expiresInSeconds)

        val builder = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withArrayClaim("roles", arrayOf("admin"))

        if (!name.isNullOrBlank()) {
            builder.withClaim("name", name)
        }

        return builder.sign(algorithm)
    }

    fun toPrincipal(payload: Payload): AppPrincipal? {
        val userId = payload.getClaim("userId").asString()
            ?.takeIf { it.isNotBlank() }
            ?: payload.subject?.takeIf { it.isNotBlank() }
            ?: return null

        val email = payload.getClaim("email").asString()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val name = payload.getClaim("name").asString()?.takeIf { it.isNotBlank() }
        val roles = payload.getClaim("roles").asList(String::class.java).orEmpty()
        return AppPrincipal(
            userId = userId,
            email = email,
            name = name,
            roles = roles
        )
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppJwtService {
            return AppJwtService(
                config = AppJwtConfig(
                    secret = EnvironmentConfig.required("JWT_SECRET", env),
                    issuer = EnvironmentConfig.required("JWT_ISSUER", env),
                    audience = EnvironmentConfig.required("JWT_AUDIENCE", env)
                )
            )
        }
    }
}
