package com.printbusinesskmp.desktop.api

import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.modelsJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.serialization.kotlinx.json.json

object DesktopApiClient {
    private val baseUrl: String = resolveBaseUrl()

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(modelsJson)
        }
    }

    suspend fun getClients(): List<Client> {
        return httpClient.get("$baseUrl/api/clients").body()
    }

    suspend fun getOrders(): List<Order> {
        return httpClient.get("$baseUrl/api/orders").body()
    }

    suspend fun getOrder(id: String): Order {
        return httpClient.get("$baseUrl/api/orders/$id").body()
    }

    suspend fun getInvoicesByOrderId(orderId: String): List<Invoice> {
        return httpClient.get("$baseUrl/api/invoices/order/$orderId").body()
    }

    suspend fun generateInvoice(orderId: String): Invoice {
        return httpClient.post("$baseUrl/api/invoices/generate/$orderId").body()
    }

    suspend fun downloadInvoicePdf(invoiceId: String): ByteArray {
        return httpClient.get("$baseUrl/api/invoices/download/$invoiceId").body()
    }

    private fun resolveBaseUrl(): String {
        val property = System.getProperty("printbusiness.api.baseUrl")
            ?.trim()
            ?.removeSuffix("/")
        if (!property.isNullOrEmpty()) {
            return property
        }

        val env = System.getenv("PRINT_BUSINESS_API_BASE_URL")
            ?.trim()
            ?.removeSuffix("/")
        if (!env.isNullOrEmpty()) {
            return env
        }

        return "http://localhost:8080"
    }
}
