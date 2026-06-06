package com.printbusinesskmp

import com.printbusinesskmp.auth.AppJwtService
import com.printbusinesskmp.auth.DatabaseAllowlistService
import com.printbusinesskmp.auth.DbFirstAllowlistService
import com.printbusinesskmp.auth.EnvAllowlistService
import com.printbusinesskmp.auth.EnvironmentConfig
import com.printbusinesskmp.auth.GoogleIdTokenVerifier
import com.printbusinesskmp.auth.configureJwtAuthentication
import com.printbusinesskmp.database.DatabaseFactory
import com.printbusinesskmp.repository.AllowedEmailRepository
import com.printbusinesskmp.routes.configureAllowlistAdminRoutes
import com.printbusinesskmp.routes.configureAuthRoutes
import com.printbusinesskmp.routes.configureBusinessProfileRoutes
import com.printbusinesskmp.routes.configureClientRoutes
import com.printbusinesskmp.routes.configureInvoiceRoutes
import com.printbusinesskmp.routes.configureLayoutRoutes
import com.printbusinesskmp.routes.configureOrderRoutes
import com.printbusinesskmp.routes.configureOutsourceRoutes
import com.printbusinesskmp.routes.configurePartnerRoutes
import com.printbusinesskmp.routes.configurePricingRoutes
import com.printbusinesskmp.routes.configureSavedItemRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    val appJwtService = AppJwtService.fromEnvironment()
    val googleIdTokenVerifier = GoogleIdTokenVerifier.fromEnvironment()
    val envAllowlistService = EnvAllowlistService.fromEnvironment()
    val dbAllowlistService = DatabaseAllowlistService(AllowedEmailRepository())
    val allowlistService = DbFirstAllowlistService(
        dbAllowlistService = dbAllowlistService,
        envAllowlistService = envAllowlistService
    )

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
    }

    val corsOrigins = EnvironmentConfig.csv("CORS_ALLOWED_ORIGINS")
    require(corsOrigins.isNotEmpty()) {
        "CORS_ALLOWED_ORIGINS environment variable must contain at least one origin"
    }

    install(CORS) {
        corsOrigins.map(::parseCorsOrigin).forEach { origin ->
            allowHost(origin.host, schemes = listOf(origin.scheme))
        }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(CallLogging)
    configureJwtAuthentication(appJwtService)

    routing {
        get("/") {
            call.respondText("Print Business API")
        }
        get("/health") {
            call.respondText("OK")
        }
        configureAuthRoutes(
            googleIdTokenVerifier = googleIdTokenVerifier,
            allowlistService = allowlistService,
            appJwtService = appJwtService
        )
        configureAllowlistAdminRoutes(
            allowlistService = allowlistService
        )

        configureClientRoutes()
        configureBusinessProfileRoutes()
        configurePartnerRoutes()
        configureOutsourceRoutes()
        configureOrderRoutes()
        configureInvoiceRoutes()
        configureSavedItemRoutes()
        configureLayoutRoutes()
        configurePricingRoutes()
    }
}

private data class CorsOrigin(
    val host: String,
    val scheme: String
)

private fun parseCorsOrigin(origin: String): CorsOrigin {
    val url = runCatching { Url(origin) }.getOrElse {
        throw IllegalArgumentException("Invalid origin in CORS_ALLOWED_ORIGINS: '$origin'")
    }

    val scheme = url.protocol.name.lowercase()
    require(scheme == "http" || scheme == "https") {
        "CORS origin must use http or https: '$origin'"
    }

    val hostWithOptionalPort = if (url.port == url.protocol.defaultPort) {
        url.host
    } else {
        "${url.host}:${url.port}"
    }
    require(hostWithOptionalPort.isNotBlank()) {
        "CORS origin host is invalid: '$origin'"
    }

    return CorsOrigin(
        host = hostWithOptionalPort,
        scheme = scheme
    )
}
