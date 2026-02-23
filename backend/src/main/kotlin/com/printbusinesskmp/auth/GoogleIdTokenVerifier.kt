package com.printbusinesskmp.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import io.ktor.http.HttpStatusCode
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.util.concurrent.TimeUnit

class GoogleIdTokenVerifier(
    private val webGoogleClientId: String,
    private val acceptedGoogleClientIds: Set<String>,
    private val jwkProvider: JwkProvider = JwkProviderBuilder(URL(GOOGLE_JWKS_URL))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build(),
    private val clock: Clock = Clock.systemUTC()
) {
    fun clientId(): String = webGoogleClientId

    fun verify(idToken: String): GoogleUserClaims {
        val token = idToken.trim()
        if (token.isEmpty()) {
            throw AuthException(
                status = HttpStatusCode.BadRequest,
                error = "invalid_request",
                message = "idToken must not be blank"
            )
        }

        val decoded = runCatching { JWT.decode(token) }.getOrElse {
            throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token format is invalid"
            )
        }

        val keyId = decoded.keyId?.takeIf { it.isNotBlank() } ?: throw AuthException(
            status = HttpStatusCode.Unauthorized,
            error = "invalid_google_token",
            message = "Google ID token is missing key id"
        )

        val publicKey = runCatching {
            jwkProvider.get(keyId).publicKey as? RSAPublicKey
        }.getOrNull() ?: throw AuthException(
            status = HttpStatusCode.Unauthorized,
            error = "invalid_google_token",
            message = "Google signing key is unavailable"
        )

        val verifier = JWT.require(Algorithm.RSA256(publicKey, null))
            .withIssuer(*GOOGLE_ISSUERS)
            .acceptLeeway(CLOCK_SKEW_SECONDS)
            .build()

        val verified = runCatching { verifier.verify(token) }.getOrElse {
            throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token verification failed"
            )
        }

        val issuedAt = verified.issuedAt ?: throw AuthException(
            status = HttpStatusCode.Unauthorized,
            error = "invalid_google_token",
            message = "Google ID token is missing issued-at claim"
        )
        if (issuedAt.toInstant().isAfter(clock.instant().plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token issued-at claim is in the future"
            )
        }

        val audiences = verified.audience
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (audiences.isEmpty() || audiences.none { it in acceptedGoogleClientIds }) {
            throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token audience is not allowed"
            )
        }

        val email = verified.getClaim("email").asString()
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?: throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token is missing email claim"
            )

        val emailVerified = parseBooleanClaim(verified.getClaim("email_verified"))
        if (!emailVerified) {
            throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "email_not_verified",
                message = "Google account email is not verified"
            )
        }

        val sub = verified.subject?.takeIf { it.isNotBlank() }
            ?: verified.getClaim("sub").asString()?.takeIf { it.isNotBlank() }
            ?: throw AuthException(
                status = HttpStatusCode.Unauthorized,
                error = "invalid_google_token",
                message = "Google ID token is missing subject claim"
            )

        return GoogleUserClaims(
            sub = sub,
            email = email,
            name = verified.getClaim("name").asString()?.trim()?.takeIf { it.isNotEmpty() },
            picture = verified.getClaim("picture").asString()?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        private const val GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs"
        private const val CLOCK_SKEW_SECONDS = 60L
        private val GOOGLE_ISSUERS = arrayOf("https://accounts.google.com", "accounts.google.com")

        fun fromEnvironment(env: Map<String, String> = System.getenv()): GoogleIdTokenVerifier {
            val webGoogleClientId = EnvironmentConfig.required("GOOGLE_CLIENT_ID", env)
            val desktopGoogleClientId = env["GOOGLE_DESKTOP_CLIENT_ID"]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            val acceptedGoogleClientIds = buildSet {
                add(webGoogleClientId)
                if (desktopGoogleClientId != null) {
                    add(desktopGoogleClientId)
                }
            }

            return GoogleIdTokenVerifier(
                webGoogleClientId = webGoogleClientId,
                acceptedGoogleClientIds = acceptedGoogleClientIds
            )
        }
    }
}

private fun parseBooleanClaim(claim: Claim): Boolean {
    claim.asBoolean()?.let { return it }
    return claim.asString()
        ?.trim()
        ?.lowercase()
        ?.toBooleanStrictOrNull()
        ?: false
}
