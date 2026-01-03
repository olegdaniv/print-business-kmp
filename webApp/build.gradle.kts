import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            implementation(projects.shared)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
            implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
        }
        jsMain.dependencies {
            implementation(kotlin("stdlib-js")) // Adds window, document, etc.
            implementation("io.ktor:ktor-client-js:${libs.versions.ktor.get()}")
        }
        wasmJsMain.dependencies {
            implementation(kotlin("stdlib-wasm-js")) // Adds wasm-specific browser refs
            implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


