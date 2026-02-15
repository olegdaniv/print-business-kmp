plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.flyway)
    application
}

group = "com.printbusinesskmp"
version = "1.0.0"
application {
    mainClass.set("com.printbusinesskmp.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)

    // Ktor server
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverCallLogging)
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.3.3")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("com.auth0:jwks-rsa:0.22.1")

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.h2)  // Keep for local development
    implementation(libs.postgres)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

ktor {
    fatJar {
        archiveFileName.set("server-all.jar")
    }
}

flyway {
    val explicitFlywayUrl = System.getenv("FLYWAY_URL")?.trim()?.takeIf { it.isNotEmpty() }
    val h2Url = System.getenv("H2_URL")?.trim()?.takeIf { it.isNotEmpty() }
    val dbHost = System.getenv("DB_HOST")?.trim()?.takeIf { it.isNotEmpty() } ?: "localhost"
    val dbPort = System.getenv("DB_PORT")?.trim()?.takeIf { it.isNotEmpty() } ?: "5432"
    val dbName = System.getenv("DB_NAME")?.trim()?.takeIf { it.isNotEmpty() } ?: "printbusiness_db"

    val resolvedUrl = explicitFlywayUrl ?: h2Url ?: "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    val isH2 = resolvedUrl.startsWith("jdbc:h2:")

    url = resolvedUrl
    user = System.getenv("FLYWAY_USER")?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (isH2) {
            System.getenv("H2_USER")?.trim()?.takeIf { it.isNotEmpty() } ?: "sa"
        } else {
            System.getenv("DB_USER")?.trim()?.takeIf { it.isNotEmpty() } ?: "printbusiness"
        }
    password = System.getenv("FLYWAY_PASSWORD")
        ?: if (isH2) {
            System.getenv("H2_PASSWORD") ?: ""
        } else {
            System.getenv("DB_PASSWORD") ?: ""
        }
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    baselineVersion = "0"
}
