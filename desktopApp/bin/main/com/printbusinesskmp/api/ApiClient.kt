package com.printbusinesskmp.api

import com.printbusinesskmp.models.BusinessProfile
import com.printbusinesskmp.models.BusinessProfileUpsertRequest
import com.printbusinesskmp.models.Client
import com.printbusinesskmp.models.ClientCreateRequest
import com.printbusinesskmp.models.ClientUpdateRequest
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.Layout
import com.printbusinesskmp.models.LayoutCreateRequest
import com.printbusinesskmp.models.LayoutStatus
import com.printbusinesskmp.models.LayoutUpdateRequest
import com.printbusinesskmp.models.Order
import com.printbusinesskmp.models.OrderCreateRequest
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.OrderUpdateRequest
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PricingRequest
import com.printbusinesskmp.models.PricingResult
import com.printbusinesskmp.models.modelsJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
private data class OrderStateRequest(
    val status: OrderStatus? = null,
    val paymentStatus: PaymentStatus? = null
)

@Serializable
private data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
private data class GoogleAuthResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val email: String,
    val name: String? = null
)

@Serializable
private data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null
)

data class AuthSession(
    val accessToken: String,
    val expiresInSeconds: Long,
    val email: String,
    val name: String? = null
)

class NotAllowlistedException(message: String) : RuntimeException(message)

class SessionExpiredException(message: String = "Session expired. Please sign in again.") : RuntimeException(message)

class AuthRequestException(
    val status: HttpStatusCode,
    message: String
) : RuntimeException(message)

object ApiClient {
    private val baseUrl: String = resolveBaseUrl()

    @Volatile
    private var accessToken: String? = null
    private var onUnauthorized: (() -> Unit)? = null

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(modelsJson)
        }
        HttpResponseValidator {
            validateResponse { response ->
                val requestPath = response.call.request.url.encodedPath
                if (response.status == HttpStatusCode.Unauthorized && requiresAppJwt(requestPath)) {
                    accessToken = null
                    onUnauthorized?.invoke()
                    throw SessionExpiredException()
                }
            }
        }
        install(DefaultRequest) {
            val requestPath = url.build().encodedPath
            if (requiresAppJwt(requestPath)) {
                accessToken?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
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

        return com.printbusinesskmp.shared.BuildKonfig.BASE_URL
    }

    fun setAccessToken(token: String?) {
        accessToken = token?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setUnauthorizedHandler(handler: (() -> Unit)?) {
        onUnauthorized = handler
    }

    suspend fun exchangeGoogleIdToken(idToken: String): AuthSession {
        val response = client.post("$baseUrl/auth/google") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GoogleAuthRequest(idToken = idToken))
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val payload = response.body<GoogleAuthResponse>()
                AuthSession(
                    accessToken = payload.accessToken,
                    expiresInSeconds = payload.expiresInSeconds,
                    email = payload.email,
                    name = payload.name
                )
            }

            HttpStatusCode.Forbidden -> {
                val message = parseErrorMessage(
                    response = response,
                    fallback = "This Google account is not allowlisted for this app."
                )
                throw NotAllowlistedException(message)
            }

            else -> {
                val message = parseErrorMessage(
                    response = response,
                    fallback = "Sign-in failed (${response.status.value})."
                )
                throw AuthRequestException(response.status, message)
            }
        }
    }

    suspend fun getBusinessProfile(): BusinessProfile? {
        val response = client.get("$baseUrl/api/business-profile")
        return if (response.status == HttpStatusCode.NotFound) {
            null
        } else {
            response.body()
        }
    }

    suspend fun upsertBusinessProfile(request: BusinessProfileUpsertRequest): BusinessProfile {
        return client.put("$baseUrl/api/business-profile") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun getClients(): List<Client> {
        return client.get("$baseUrl/api/clients").body()
    }

    suspend fun getClient(id: String): Client {
        return client.get("$baseUrl/api/clients/$id").body()
    }

    suspend fun createClient(request: ClientCreateRequest): Client {
        return client.post("$baseUrl/api/clients") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateClient(id: String, request: ClientUpdateRequest): Client {
        return client.put("$baseUrl/api/clients/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun deleteClient(id: String) {
        client.delete("$baseUrl/api/clients/$id")
    }

    suspend fun getOrders(): List<Order> {
        return client.get("$baseUrl/api/orders").body()
    }

    suspend fun getOrder(id: String): Order {
        return client.get("$baseUrl/api/orders/$id").body()
    }

    suspend fun createOrder(request: OrderCreateRequest): Order {
        return client.post("$baseUrl/api/orders") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateOrder(id: String, request: OrderUpdateRequest): Order {
        return client.put("$baseUrl/api/orders/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateOrderState(
        id: String,
        status: OrderStatus? = null,
        paymentStatus: PaymentStatus? = null
    ): Order {
        return client.patch("$baseUrl/api/orders/$id/state") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(OrderStateRequest(status = status, paymentStatus = paymentStatus))
        }.body()
    }

    suspend fun deleteOrder(id: String) {
        client.delete("$baseUrl/api/orders/$id")
    }

    suspend fun calculatePricing(request: PricingRequest): PricingResult {
        return client.post("$baseUrl/api/pricing/calculate") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun generateInvoice(orderId: String): Invoice {
        return client.post("$baseUrl/api/invoices/generate/$orderId").body()
    }

    suspend fun getAllInvoices(): List<Invoice> {
        return client.get("$baseUrl/api/invoices").body()
    }

    suspend fun getInvoicesByOrderId(orderId: String): List<Invoice> {
        return client.get("$baseUrl/api/invoices/order/$orderId").body()
    }

    suspend fun getInvoice(id: String): Invoice {
        return client.get("$baseUrl/api/invoices/$id").body()
    }

    suspend fun deleteInvoice(id: String) {
        client.delete("$baseUrl/api/invoices/$id")
    }

    suspend fun downloadInvoicePdf(id: String): ByteArray {
        return client.get("$baseUrl/api/invoices/download/$id").body()
    }

    fun getInvoiceDownloadUrl(id: String): String {
        return "$baseUrl/api/invoices/download/$id"
    }

    suspend fun getLayouts(
        search: String? = null,
        clientId: String? = null,
        status: LayoutStatus? = null
    ): List<Layout> {
        return client.get("$baseUrl/api/layouts") {
            url {
                search?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                    parameters.append("search", value)
                }
                clientId?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
                    parameters.append("clientId", value)
                }
                status?.let { value ->
                    parameters.append("status", value.name)
                }
            }
        }.body()
    }

    suspend fun getLayout(id: String): Layout {
        return client.get("$baseUrl/api/layouts/$id").body()
    }

    suspend fun createLayout(request: LayoutCreateRequest): Layout {
        return client.post("$baseUrl/api/layouts") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun updateLayout(id: String, request: LayoutUpdateRequest): Layout {
        return client.put("$baseUrl/api/layouts/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }

    suspend fun deleteLayout(id: String) {
        client.delete("$baseUrl/api/layouts/$id")
    }

    private fun requiresAppJwt(path: String): Boolean {
        return path.startsWith("/api/") || path.startsWith("/admin/")
    }

    private suspend fun parseErrorMessage(response: HttpResponse, fallback: String): String {
        val bodyText = response.bodyAsText()
        if (bodyText.isBlank()) return fallback
        return runCatching {
            modelsJson.decodeFromString<ApiErrorResponse>(bodyText).message?.ifBlank { null }
        }.getOrNull() ?: fallback
    }
}
