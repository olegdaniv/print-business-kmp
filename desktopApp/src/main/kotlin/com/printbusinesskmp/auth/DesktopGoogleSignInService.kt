package com.printbusinesskmp.auth

import com.printbusinesskmp.models.modelsJson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.awt.Desktop
import java.net.BindException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class DesktopGoogleSignInService(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(modelsJson)
        }
    }
) {
    suspend fun requestIdToken(): String = withContext(Dispatchers.IO) {
        val clientId = resolveGoogleClientId()
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val expectedState = UUID.randomUUID().toString()
        val redirectHost = resolveRedirectHost()
        val preferredRedirectPort = resolveRedirectPort()

        val callbackDeferred = CompletableDeferred<OAuthCallback>()
        val loopbackServer = try {
            HttpServer.create(InetSocketAddress("127.0.0.1", preferredRedirectPort), 0)
        } catch (error: BindException) {
            throw IllegalStateException(
                "Google sign-in failed because redirect port $preferredRedirectPort is already in use."
            )
        }
        val redirectUri = "http://$redirectHost:${loopbackServer.address.port}/"

        loopbackServer.createContext("/") { exchange ->
            handleCallback(exchange, callbackDeferred)
        }
        loopbackServer.start()

        try {
            val authorizationUrl = buildAuthorizationUrl(
                clientId = clientId,
                redirectUri = redirectUri,
                state = expectedState,
                codeChallenge = codeChallenge
            )
            openBrowser(authorizationUrl)

            val callback = try {
                withTimeout(CALLBACK_TIMEOUT_MILLIS) {
                    callbackDeferred.await()
                }
            } catch (_: TimeoutCancellationException) {
                throw IllegalStateException(
                    "Google sign-in callback timed out. If the browser showed " +
                        "'redirect_uri_mismatch', either use a Google OAuth Desktop client ID " +
                        "for desktopGoogleClientId/GOOGLE_DESKTOP_CLIENT_ID or register this " +
                        "redirect URI on the OAuth client: $redirectUri (client_id=$clientId). " +
                        "For Web OAuth clients, set a fixed GOOGLE_DESKTOP_REDIRECT_PORT and " +
                        "register that exact URI."
                )
            }
            if (!callback.error.isNullOrBlank()) {
                throw IllegalStateException("Google sign-in canceled: ${callback.error}")
            }
            if (callback.state != expectedState) {
                throw IllegalStateException("Google sign-in failed because OAuth state did not match.")
            }
            val code = callback.code?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Google sign-in failed because authorization code was missing.")

            exchangeCodeForIdToken(
                clientId = clientId,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier,
                code = code
            )
        } finally {
            loopbackServer.stop(0)
        }
    }

    private suspend fun exchangeCodeForIdToken(
        clientId: String,
        redirectUri: String,
        codeVerifier: String,
        code: String
    ): String {
        val response = client.submitForm(
            url = GOOGLE_TOKEN_ENDPOINT,
            formParameters = Parameters.build {
                append("client_id", clientId)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri)
                append("code_verifier", codeVerifier)
                append("code", code)
            }
        )

        val payloadText = response.body<String>()
        if (!response.status.isSuccess()) {
            val error = runCatching {
                modelsJson.decodeFromString<GoogleTokenErrorResponse>(payloadText)
            }.getOrNull()
            val reason = error?.errorDescription?.takeIf { it.isNotBlank() }
                ?: error?.error?.takeIf { it.isNotBlank() }
                ?: payloadText
            throw IllegalStateException("Google token exchange failed: $reason")
        }

        val tokenResponse = runCatching {
            modelsJson.decodeFromString<GoogleTokenSuccessResponse>(payloadText)
        }.getOrElse {
            throw IllegalStateException("Google token response is invalid")
        }

        return tokenResponse.idToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google token response did not include id_token")
    }

    private fun buildAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String
    ): String {
        val encodedRedirect = urlEncode(redirectUri)
        val encodedClientId = urlEncode(clientId)
        val encodedState = urlEncode(state)
        val encodedCodeChallenge = urlEncode(codeChallenge)
        val encodedScope = urlEncode("openid email profile")
        return buildString {
            append("https://accounts.google.com/o/oauth2/v2/auth")
            append("?client_id=$encodedClientId")
            append("&redirect_uri=$encodedRedirect")
            append("&response_type=code")
            append("&scope=$encodedScope")
            append("&state=$encodedState")
            append("&code_challenge=$encodedCodeChallenge")
            append("&code_challenge_method=S256")
            append("&access_type=offline")
            append("&prompt=select_account")
        }
    }

    private fun handleCallback(exchange: HttpExchange, callbackDeferred: CompletableDeferred<OAuthCallback>) {
        val params = parseQuery(exchange.requestURI.rawQuery.orEmpty())
        val callback = OAuthCallback(
            code = params["code"],
            error = params["error"],
            state = params["state"]
        )
        callbackDeferred.complete(callback)

        val html = """
            <html>
            <head><title>Print Business Login</title></head>
            <body style="font-family:sans-serif;padding:24px;">
              <h3>Sign-in completed</h3>
              <p>You can close this window and return to Print Business.</p>
            </body>
            </html>
        """.trimIndent()
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { stream ->
            stream.write(bytes)
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&')
            .mapNotNull { part ->
                val pieces = part.split('=', limit = 2)
                if (pieces.isEmpty()) return@mapNotNull null
                val key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8)
                val value = URLDecoder.decode(pieces.getOrElse(1) { "" }, StandardCharsets.UTF_8)
                key to value
            }
            .toMap()
    }

    private fun openBrowser(url: String) {
        check(Desktop.isDesktopSupported()) {
            "Desktop browser integration is not available on this OS."
        }
        Desktop.getDesktop().browse(URI(url))
    }

    private fun resolveGoogleClientId(): String {
        val property = System.getProperty("printbusiness.google.clientId")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (property != null) return property

        val env = System.getenv("GOOGLE_DESKTOP_CLIENT_ID")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (env != null) return env

        throw IllegalStateException(
            "Google Desktop client ID is missing. Set -Dprintbusiness.google.clientId or GOOGLE_DESKTOP_CLIENT_ID."
        )
    }

    private fun resolveRedirectHost(): String {
        val property = System.getProperty("printbusiness.google.redirectHost")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (property != null) return property

        val env = System.getenv("GOOGLE_DESKTOP_REDIRECT_HOST")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (env != null) return env

        return "localhost"
    }

    private fun resolveRedirectPort(): Int {
        val property = System.getProperty("printbusiness.google.redirectPort")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val env = System.getenv("GOOGLE_DESKTOP_REDIRECT_PORT")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val raw = property ?: env ?: return 0

        val port = raw.toIntOrNull()
            ?: throw IllegalStateException("Google redirect port is invalid: '$raw'")
        if (port !in 0..65535) {
            throw IllegalStateException("Google redirect port is out of range: '$raw'")
        }
        return port
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun urlEncode(value: String): String {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private data class OAuthCallback(
        val code: String?,
        val error: String?,
        val state: String?
    )

    @Serializable
    private data class GoogleTokenSuccessResponse(
        @SerialName("id_token")
        val idToken: String? = null
    )

    @Serializable
    private data class GoogleTokenErrorResponse(
        val error: String? = null,
        @SerialName("error_description")
        val errorDescription: String? = null
    )

    companion object {
        private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val CALLBACK_TIMEOUT_MILLIS = 180_000L
    }
}
