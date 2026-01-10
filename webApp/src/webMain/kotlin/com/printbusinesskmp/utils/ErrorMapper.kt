package com.printbusinesskmp.utils

import com.printbusinesskmp.models.ApiError
import org.jetbrains.compose.resources.StringResource
import com.printbusinesskmp.shared.resources.Res
import com.printbusinesskmp.shared.resources.error_calculation_failed
import com.printbusinesskmp.shared.resources.error_client_load_failed
import com.printbusinesskmp.shared.resources.error_forbidden
import com.printbusinesskmp.shared.resources.error_invalid_credentials
import com.printbusinesskmp.shared.resources.error_network
import com.printbusinesskmp.shared.resources.error_not_found
import com.printbusinesskmp.shared.resources.error_order_create_failed
import com.printbusinesskmp.shared.resources.error_please_add_item
import com.printbusinesskmp.shared.resources.error_please_select_client
import com.printbusinesskmp.shared.resources.error_server_error
import com.printbusinesskmp.shared.resources.error_unauthorized
import com.printbusinesskmp.shared.resources.error_unknown
import com.printbusinesskmp.shared.resources.error_user_not_found

/**
 * ErrorMapper maps API error codes to localized string resources.
 * This follows the Keys-First architecture where the backend returns stable
 * error codes, and the frontend maps them to user-facing localized messages.
 */
object ErrorMapper {

    /**
     * Maps an ApiError error code to the corresponding string resource.
     * Falls back to a generic error message if the code is not recognized.
     */
    fun mapErrorCodeToResource(errorCode: String): StringResource {
        return when (errorCode) {
            ApiError.USER_NOT_FOUND -> Res.string.error_user_not_found
            ApiError.CLIENT_LOAD_FAILED -> Res.string.error_client_load_failed
            ApiError.ORDER_CREATE_FAILED -> Res.string.error_order_create_failed
            ApiError.PLEASE_SELECT_CLIENT -> Res.string.error_please_select_client
            ApiError.PLEASE_ADD_ITEM -> Res.string.error_please_add_item
            ApiError.CALCULATION_FAILED -> Res.string.error_calculation_failed
            ApiError.NETWORK_ERROR -> Res.string.error_network
            ApiError.INVALID_CREDENTIALS -> Res.string.error_invalid_credentials
            ApiError.UNAUTHORIZED -> Res.string.error_unauthorized
            ApiError.FORBIDDEN -> Res.string.error_forbidden
            ApiError.NOT_FOUND -> Res.string.error_not_found
            ApiError.SERVER_ERROR -> Res.string.error_server_error
            else -> Res.string.error_unknown
        }
    }

    /**
     * Helper function to map an ApiError object directly to a string resource.
     */
    fun mapToResource(error: ApiError): StringResource {
        return mapErrorCodeToResource(error.errorCode)
    }
}