package com.printbusinesskmp.api

import com.printbusinesskmp.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class UpdateStatusRequest(val status: String)

object ApiClient {
    private const val BASE_URL = "http://localhost:8080"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // Client API functions
    suspend fun getClients(): List<Client> {
        return try {
            val response = client.get("$BASE_URL/api/clients")
            println("Response status: ${response.status}")
            response.body()
        } catch (e: Exception) {
            println("NETWORK ERROR: ${e.message}")
            throw e
        }
    }

    suspend fun getClient(id: String): Client {
        return client.get("$BASE_URL/api/clients/$id").body()
    }

    suspend fun createClient(request: ClientCreateRequest): Client {
        return this.client.post("$BASE_URL/api/clients") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateClient(id: String, request: ClientUpdateRequest): Client {
        return this.client.put("$BASE_URL/api/clients/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteClient(id: String) {
        client.delete("$BASE_URL/api/clients/$id")
    }

    // Order API functions
    suspend fun getOrders(): List<Order> {
        return client.get("$BASE_URL/api/orders").body()
    }

    suspend fun getOrder(id: String): Order {
        return client.get("$BASE_URL/api/orders/$id").body()
    }

    suspend fun createOrder(order: Order): Order {
        return client.post("$BASE_URL/api/orders") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }.body()
    }

    suspend fun updateOrderStatus(id: String, status: OrderStatus): Order {
        return client.put("$BASE_URL/api/orders/$id/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateStatusRequest(status = status.name))
        }.body()
    }
}