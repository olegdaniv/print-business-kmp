package com.printbusinesskmp.models

import kotlinx.serialization.json.Json

val modelsJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
}