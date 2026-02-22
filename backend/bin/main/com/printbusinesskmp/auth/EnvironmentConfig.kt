package com.printbusinesskmp.auth

object EnvironmentConfig {
    fun required(name: String, env: Map<String, String> = System.getenv()): String {
        val value = env[name]?.trim()
        require(!value.isNullOrEmpty()) { "$name environment variable is required" }
        return value
    }

    fun csv(name: String, env: Map<String, String> = System.getenv()): List<String> {
        return env[name]
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
