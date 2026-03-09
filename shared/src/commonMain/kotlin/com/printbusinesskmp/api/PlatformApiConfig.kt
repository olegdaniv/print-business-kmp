package com.printbusinesskmp.api

/**
 * Platform-specific base URL override.
 * On JVM: reads from system properties and environment variables.
 * On JS/Wasm: returns null (falls through to BuildKonfig).
 */
internal expect fun platformResolveBaseUrl(): String?
