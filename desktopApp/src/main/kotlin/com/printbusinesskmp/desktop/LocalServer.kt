package com.printbusinesskmp.desktop

import com.printbusinesskmp.localModule
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking

/**
 * Starts the backend in-process on a loopback ephemeral port so the desktop app
 * works fully offline against a local H2 database. Returns the base URL that
 * [com.printbusinesskmp.api.ApiClient] should target.
 */
object LocalServer {
    fun start(): String {
        val server = embeddedServer(Netty, port = 0, host = "127.0.0.1") {
            localModule()
        }
        server.start(wait = false)
        val port = runBlocking { server.engine.resolvedConnectors() }.first().port
        return "http://127.0.0.1:$port"
    }
}
