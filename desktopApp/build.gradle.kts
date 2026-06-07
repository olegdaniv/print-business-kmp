import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.printbusinesskmp"

val appName = "SouvenirPrint"
val appVendor = "SouvenirPrint"
val appDescription = "Souvenir printing management desktop application"
val appVersion = providers.gradleProperty("desktopAppVersion").orElse("1.0.0").get()
val updateFeedUrl = providers.gradleProperty("desktopUpdateFeedUrl")
    .orElse("https://example.com/printbusiness/updates/latest.json")
    .get()
val updateAllowedHosts: String =
    providers.gradleProperty("desktopUpdateAllowedHosts").orElse("").get()
val allowUpdatesWithoutChecksum = providers.gradleProperty("desktopAllowUpdatesWithoutChecksum")
    .orElse("true")
    .get()
    .toBoolean()

val envFromFile = File(rootDir, ".env")
    .takeIf { it.exists() }
    ?.readLines()
    ?.asSequence()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
    ?.associate { line ->
        val parts = line.split("=", limit = 2)
        parts[0].trim() to parts[1].trim()
    }
    ?: emptyMap()

fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { !it.isNullOrBlank() }?.trim()

val googleClientId: String = firstNonBlank(
    providers.gradleProperty("desktopGoogleClientId").orNull,
    System.getenv("GOOGLE_DESKTOP_CLIENT_ID"),
    envFromFile["GOOGLE_DESKTOP_CLIENT_ID"]
).orEmpty()

val googleRedirectHost: String? = firstNonBlank(
    providers.gradleProperty("desktopGoogleRedirectHost").orNull,
    System.getenv("GOOGLE_DESKTOP_REDIRECT_HOST"),
    envFromFile["GOOGLE_DESKTOP_REDIRECT_HOST"]
)

val googleRedirectPort: String? = firstNonBlank(
    providers.gradleProperty("desktopGoogleRedirectPort").orNull,
    System.getenv("GOOGLE_DESKTOP_REDIRECT_PORT"),
    envFromFile["GOOGLE_DESKTOP_REDIRECT_PORT"]
)

val googleClientSecret: String? = firstNonBlank(
    providers.gradleProperty("desktopGoogleClientSecret").orNull,
    System.getenv("GOOGLE_DESKTOP_CLIENT_SECRET"),
    envFromFile["GOOGLE_DESKTOP_CLIENT_SECRET"]
)

version = appVersion

dependencies {
    implementation(projects.shared)
    // Embedded local backend (runs in-process so desktop uses a local H2 database)
    implementation(projects.backend)

    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.compose.ui:ui:1.11.1")
    implementation("org.jetbrains.compose.components:components-resources:1.11.1")
    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // Embedded Ktor server (runs the backend in-process for the local DB)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)

    // CIO engine for desktop Google OAuth (DesktopGoogleSignInService uses its own HttpClient)
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")

    // PDF generation (same version as backend)
    implementation("com.itextpdf:itext7-core:7.2.5")
}

tasks.named<ProcessResources>("processResources") {
    from("$rootDir/backend/src/main/resources/fonts") {
        into("fonts")
    }
}

compose.desktop {
    application {
        mainClass = "com.printbusinesskmp.desktop.MainKt"
        jvmArgs += mutableListOf(
            "-Dprintbusiness.app.name=$appName",
            "-Dprintbusiness.app.version=$appVersion",
            "-Dprintbusiness.update.feedUrl=$updateFeedUrl",
            "-Dprintbusiness.update.allowedHosts=$updateAllowedHosts",
            "-Dprintbusiness.update.allowWithoutChecksum=$allowUpdatesWithoutChecksum"
        )
        if (googleClientId.isNotBlank()) {
            jvmArgs += "-Dprintbusiness.google.clientId=$googleClientId"
        }
        if (!googleRedirectHost.isNullOrBlank()) {
            jvmArgs += "-Dprintbusiness.google.redirectHost=$googleRedirectHost"
        }
        if (!googleRedirectPort.isNullOrBlank()) {
            jvmArgs += "-Dprintbusiness.google.redirectPort=$googleRedirectPort"
        }
        if (!googleClientSecret.isNullOrBlank()) {
            jvmArgs += "-Dprintbusiness.google.clientSecret=$googleClientSecret"
        }
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = appName
            packageVersion = appVersion
            vendor = appVendor
            description = appDescription

            modules(
                "java.net.http",
                "jdk.crypto.ec",
                "jdk.httpserver"
            )

            windows {
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "f2c8732e-45b6-4f9b-b90e-7c94c0d7fa4b"
                iconFile.set(project.file("src/main/resources/installer/windows/printbusiness.ico"))
            }

            macOS {
                bundleID = "com.printbusinesskmp.desktop"
                iconFile.set(project.file("src/main/resources/installer/mac/printbusiness.icns"))
            }

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

tasks.register("printMsiSha256") {
    group = "distribution"
    description = "Builds MSI and prints SHA-256 checksum for release feed."
    dependsOn("packageMsi")
    doLast {
        val msiDir = layout.buildDirectory.dir("compose/binaries/main/msi").get().asFile
        val msiFile = fileTree(msiDir) {
            include("**/*.msi")
        }.files.firstOrNull() ?: error("No MSI found in $msiDir")

        val digest = MessageDigest.getInstance("SHA-256")
        msiFile.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest().joinToString("") { byte: Byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
        println("${msiFile.name}  $hash")
    }
}
