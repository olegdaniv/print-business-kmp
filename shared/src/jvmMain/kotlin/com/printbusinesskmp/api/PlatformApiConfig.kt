package com.printbusinesskmp.api

internal actual fun platformResolveBaseUrl(): String? {
    val property = System.getProperty("printbusiness.api.baseUrl")
        ?.trim()
        ?.removeSuffix("/")
    if (!property.isNullOrEmpty()) return property

    val env = System.getenv("PRINT_BUSINESS_API_BASE_URL")
        ?.trim()
        ?.removeSuffix("/")
    if (!env.isNullOrEmpty()) return env

    return null
}
