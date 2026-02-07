package com.printbusinesskmp

import com.printbusinesskmp.database.DatabaseFactory
import com.printbusinesskmp.routes.configureBusinessProfileRoutes
import com.printbusinesskmp.routes.configureClientRoutes
import com.printbusinesskmp.routes.configureInvoiceRoutes
import com.printbusinesskmp.routes.configureOrderRoutes
import com.printbusinesskmp.routes.configureOutsourceRoutes
import com.printbusinesskmp.routes.configurePartnerRoutes
import com.printbusinesskmp.routes.configurePricingRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
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
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

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

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
    }

    install(CallLogging)

    routing {
        get("/") {
            call.respondText("Print Business API")
        }
        get("/health") {
            call.respondText("OK")
        }

        configureClientRoutes()
        configureBusinessProfileRoutes()
        configurePartnerRoutes()
        configureOutsourceRoutes()
        configureOrderRoutes()
        configureInvoiceRoutes()
        configurePricingRoutes()
    }
}
