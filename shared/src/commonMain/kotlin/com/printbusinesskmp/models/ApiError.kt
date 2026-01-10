package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

/**
 * ApiError represents errors returned from the API using the Keys-First architecture.
 * The backend returns stable error codes (e.g., "USER_NOT_FOUND") instead of
 * user-facing messages. The frontend maps these codes to localized strings.
 */
@Serializable
data class ApiError(
    val errorCode: String,
    val details: Map<String, String>? = null,
    val timestamp: String? = null
) {
    companion object {
        // Error Codes Constants
        const val USER_NOT_FOUND = "USER_NOT_FOUND"
        const val CLIENT_LOAD_FAILED = "CLIENT_LOAD_FAILED"
        const val ORDER_CREATE_FAILED = "ORDER_CREATE_FAILED"
        const val PLEASE_SELECT_CLIENT = "PLEASE_SELECT_CLIENT"
        const val PLEASE_ADD_ITEM = "PLEASE_ADD_ITEM"
        const val CALCULATION_FAILED = "CALCULATION_FAILED"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
        const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val FORBIDDEN = "FORBIDDEN"
        const val NOT_FOUND = "NOT_FOUND"
        const val SERVER_ERROR = "SERVER_ERROR"
    }
}

/**
 * ApiResponse wraps API responses with success/error states
 */
@Serializable
sealed class ApiResponse<out T> {
    @Serializable
    data class Success<T>(val data: T) : ApiResponse<T>()

    @Serializable
    data class Error(val error: ApiError) : ApiResponse<Nothing>()
}