package com.printbusinesskmp

import com.printbusinesskmp.database.DatabaseFactory
import com.printbusinesskmp.routes.configureClientRoutes
import com.printbusinesskmp.routes.configureOrderRoutes
import com.printbusinesskmp.routes.configurePricingRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Configure plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost() // Allow all origins for development
        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
        anyHost() // For development only!
    }

    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
    }

    // Configure routing
    routing {
        get("/") {
            call.respondText("Print Business API - Server is running")
        }

        get("/health") {
            call.respondText("OK")
        }

        configureClientRoutes()
        configureOrderRoutes()
        configurePricingRoutes()
    }
}