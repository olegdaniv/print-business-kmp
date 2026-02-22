import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.printbusinesskmp.shared.resources"
    generateResClass = always
}

buildkonfig {
    packageName = "com.printbusinesskmp.shared"
    defaultConfigs {
        val envFile = File(project.rootDir, ".env")
        val envFromFile = if (envFile.exists()) {
            envFile.readLines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .associate { line ->
                    val parts = line.split("=", limit = 2)
                    parts[0].trim() to parts[1].trim()
                }
        } else {
            emptyMap()
        }

        val host = project.findProperty("printbusiness.api.host")?.toString() ?: "localhost"
        val port = project.findProperty("printbusiness.api.port")?.toString() ?: "8080"
        val scheme = project.findProperty("printbusiness.api.scheme")?.toString() ?: "http"
        val webGoogleClientId = project.findProperty("printbusiness.google.clientId")
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: System.getenv("GOOGLE_CLIENT_ID")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: envFromFile["GOOGLE_CLIENT_ID"]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: ""

        val portSuffix = if (port == "80" || port == "443" || port.isBlank()) "" else ":$port"
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "BASE_URL",
            "$scheme://$host$portSuffix"
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "WEB_GOOGLE_CLIENT_ID",
            webGoogleClientId
        )
    }
}
