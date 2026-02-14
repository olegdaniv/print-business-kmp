import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "com.printbusinesskmp"

val appName = "PrintBusiness"
val appVendor = "PrintBusiness"
val appDescription = "Print business management desktop application"
val appVersion = providers.gradleProperty("desktopAppVersion").orElse("1.0.0").get()
val updateFeedUrl = providers.gradleProperty("desktopUpdateFeedUrl")
    .orElse("https://example.com/printbusiness/updates/latest.json")
    .get()
val updateAllowedHosts = providers.gradleProperty("desktopUpdateAllowedHosts").orElse("").get()
val allowUpdatesWithoutChecksum = providers.gradleProperty("desktopAllowUpdatesWithoutChecksum")
    .orElse("true")
    .get()
    .toBoolean()

version = appVersion

dependencies {
    implementation(projects.shared)

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
}

compose.desktop {
    application {
        mainClass = "com.printbusinesskmp.desktop.MainKt"
        jvmArgs += listOf(
            "-Dprintbusiness.app.name=$appName",
            "-Dprintbusiness.app.version=$appVersion",
            "-Dprintbusiness.update.feedUrl=$updateFeedUrl",
            "-Dprintbusiness.update.allowedHosts=$updateAllowedHosts",
            "-Dprintbusiness.update.allowWithoutChecksum=$allowUpdatesWithoutChecksum"
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = appName
            packageVersion = appVersion
            vendor = appVendor
            description = appDescription

            windows {
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "f2c8732e-45b6-4f9b-b90e-7c94c0d7fa4b"
                iconFile.set(project.file("src/main/resources/installer/windows/printbusiness.ico"))
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
