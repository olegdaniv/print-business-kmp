package com.printbusinesskmp.api

import com.printbusinesskmp.models.BusinessProfile
import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.PricingResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OrderStateRequest(
    val status: OrderStatus? = null,
    val paymentStatus: PaymentStatus? = null
)

object ApiClient {
    private const val BASE_URL = "http://localhost:8080"

    private val client = HttpClient {
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
    }

    suspend fun getBusinessProfile(): BusinessProfile? {
        val response = client.get("$BASE_URL/api/business-profile")
        return if (response.status == HttpStatusCode.NotFound) {
            null
        } else {
            response.body()
        }
    }

    suspend fun upsertBusinessProfile(request: BusinessProfileUpsertRequest): BusinessProfile {
        return client.put("$BASE_URL/api/business-profile") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun getClients(): List<Client> {
        return client.get("$BASE_URL/api/clients").body()
    }

    suspend fun getClient(id: String): Client {
        return client.get("$BASE_URL/api/clients/$id").body()
    }

    suspend fun createClient(request: ClientCreateRequest): Client {
        return client.post("$BASE_URL/api/clients") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateClient(id: String, request: ClientUpdateRequest): Client {
        return client.put("$BASE_URL/api/clients/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun deleteClient(id: String) {
        client.delete("$BASE_URL/api/clients/$id")
    }

    suspend fun getOrders(): List<Order> {
        return client.get("$BASE_URL/api/orders").body()
    }

    suspend fun getOrder(id: String): Order {
        return client.get("$BASE_URL/api/orders/$id").body()
    }

    suspend fun createOrder(request: OrderCreateRequest): Order {
        return client.post("$BASE_URL/api/orders") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateOrder(id: String, request: OrderUpdateRequest): Order {
        return client.put("$BASE_URL/api/orders/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateOrderState(
        id: String,
        status: OrderStatus? = null,
        paymentStatus: PaymentStatus? = null
    ): Order {
        return client.patch("$BASE_URL/api/orders/$id/state") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(OrderStateRequest(status = status, paymentStatus = paymentStatus))
        }.body()
    }

    suspend fun deleteOrder(id: String) {
        client.delete("$BASE_URL/api/orders/$id")
    }

    suspend fun calculatePricing(request: PricingRequest): PricingResult {
        return client.post("$BASE_URL/api/pricing/calculate") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun generateInvoice(orderId: String): Invoice {
        return client.post("$BASE_URL/api/invoices/generate/$orderId")
            .body()
    }

    suspend fun getAllInvoices(): List<Invoice> {
        return client.get("$BASE_URL/api/invoices").body()
    }

    suspend fun getInvoicesByOrderId(orderId: String): List<Invoice> {
        return client.get("$BASE_URL/api/invoices/order/$orderId").body()
    }

    suspend fun getInvoice(id: String): Invoice {
        return client.get("$BASE_URL/api/invoices/$id").body()
    }

    suspend fun deleteInvoice(id: String) {
        client.delete("$BASE_URL/api/invoices/$id")
    }

    fun getInvoiceDownloadUrl(id: String): String {
        return "$BASE_URL/api/invoices/download/$id"
    }
}
