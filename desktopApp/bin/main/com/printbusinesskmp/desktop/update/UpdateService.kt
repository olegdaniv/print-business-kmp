package com.printbusinesskmp.desktop.update

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import javax.net.ssl.SSLException
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.system.exitProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UpdateService(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val nowProvider: () -> Instant = { Instant.now() }
) {
    private val isWindows = System.getProperty("os.name")
        .orEmpty()
        .lowercase()
        .contains("win")

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val updatesDirectory: Path = Paths.get(
        System.getProperty("java.io.tmpdir"),
        AppBuildConfig.APP_NAME
    ).toAbsolutePath().normalize()

    private val configuredAllowedHosts: Set<String> = AppBuildConfig.UPDATE_ALLOWED_HOSTS
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var latestRelease: UpdateRelease? = null
    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    fun checkForUpdates() {
        if (!isWindows) {
            _uiState.update {
                it.copy(
                    errorMessage = "Оновлення MSI підтримуються лише у Windows-збірці.",
                    lastCheckedAt = nowProvider()
                )
            }
            return
        }

        if (checkJob?.isActive == true) return

        checkJob = scope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    errorMessage = null
                )
            }

            try {
                val feedUri = parseHttpsUri(AppBuildConfig.UPDATE_FEED_URL, "URL стрічки оновлень")
                val allowedHosts = resolveAllowedHosts(feedUri)
                ensureHostAllowed(feedUri, allowedHosts)

                val release = fetchRelease(feedUri, allowedHosts)
                val updateAvailable = isNewerVersion(release.version, AppBuildConfig.VERSION)
                val warningMessage = buildChecksumWarning(release)

                latestRelease = release
                _uiState.update {
                    it.copy(
                        latestVersion = release.version,
                        releaseNotes = release.notes,
                        updateAvailable = updateAvailable,
                        lastCheckedAt = nowProvider(),
                        isChecking = false,
                        errorMessage = null,
                        warningMessage = warningMessage,
                        downloadedInstaller = null,
                        downloadedBytes = 0L,
                        totalBytes = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        lastCheckedAt = nowProvider(),
                        errorMessage = toUserMessage(error)
                    )
                }
            }
        }
    }

    fun downloadLatestUpdate() {
        if (!isWindows) {
            _uiState.update { it.copy(errorMessage = "Завантаження MSI доступне лише у Windows.") }
            return
        }

        if (downloadJob?.isActive == true) return

        val release = latestRelease
        if (release == null) {
            _uiState.update { it.copy(errorMessage = "Спочатку перевірте наявність оновлень.") }
            return
        }
        if (!_uiState.value.updateAvailable) {
            _uiState.update { it.copy(errorMessage = "Оновлення наразі недоступне.") }
            return
        }

        downloadJob = scope.launch {
            val installerPath = resolveInstallerPath(release.version)
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadedBytes = 0L,
                    totalBytes = null,
                    downloadedInstaller = null,
                    errorMessage = null
                )
            }

            try {
                Files.createDirectories(updatesDirectory)
                cleanupOldInstallers(keepFileName = installerPath.name)

                downloadInstaller(release, installerPath)
                verifyChecksumIfNeeded(release, installerPath)

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadedInstaller = installerPath,
                        downloadedBytes = installerPath.fileSize(),
                        totalBytes = installerPath.fileSize(),
                        errorMessage = null
                    )
                }
            } catch (cancelled: CancellationException) {
                installerPath.deleteIfExists()
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadedBytes = 0L,
                        totalBytes = null,
                        errorMessage = "Завантаження скасовано."
                    )
                }
            } catch (error: Exception) {
                installerPath.deleteIfExists()
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadedBytes = 0L,
                        totalBytes = null,
                        errorMessage = toUserMessage(error)
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun installDownloadedUpdateAndExit() {
        if (!isWindows) {
            _uiState.update { it.copy(errorMessage = "Встановлення MSI доступне лише у Windows.") }
            return
        }

        val installer = _uiState.value.downloadedInstaller
        if (installer == null) {
            _uiState.update { it.copy(errorMessage = "Інсталятор не знайдено. Завантажте оновлення ще раз.") }
            return
        }

        if (!installer.exists() || !installer.isRegularFile()) {
            _uiState.update { it.copy(errorMessage = "Файл оновлення відсутній. Спробуйте завантажити повторно.") }
            return
        }

        val normalizedInstaller = installer.toAbsolutePath().normalize()
        if (normalizedInstaller.extension.lowercase() != "msi") {
            _uiState.update { it.copy(errorMessage = "Оновлення має бути у форматі MSI.") }
            return
        }
        if (!normalizedInstaller.startsWith(updatesDirectory)) {
            _uiState.update { it.copy(errorMessage = "Небезпечний шлях до інсталятора. Оновлення скасовано.") }
            return
        }

        try {
            ProcessBuilder("msiexec", "/i", normalizedInstaller.toString()).start()
            exitProcess(0)
        } catch (error: Exception) {
            _uiState.update { it.copy(errorMessage = "Не вдалося запустити інсталятор: ${error.message}") }
        }
    }

    private fun fetchRelease(feedUri: URI, allowedHosts: Set<String>): UpdateRelease {
        val request = HttpRequest.newBuilder(feedUri)
            .GET()
            .timeout(Duration.ofSeconds(20))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Сервер оновлень повернув HTTP ${response.statusCode()}.")
        }

        val release = parseRelease(response.body())
        parseSemVerOrThrow(release.version)

        val artifactUri = parseHttpsUri(release.windowsUrl, "URL Windows-інсталятора")
        ensureHostAllowed(artifactUri, allowedHosts)
        if (!artifactUri.path.lowercase().endsWith(".msi")) {
            throw IllegalArgumentException("URL оновлення має вказувати на файл .msi.")
        }

        release.sha256?.let(::validateSha256)
        if (release.sha256 == null && !AppBuildConfig.ALLOW_UPDATES_WITHOUT_CHECKSUM) {
            throw IllegalArgumentException("Оновлення без SHA-256 заборонені конфігурацією.")
        }

        return release
    }

    private fun parseRelease(rawJson: String): UpdateRelease {
        val root = Json.parseToJsonElement(rawJson).jsonObject
        val version = root.requiredString("version")
        val notes = root.optionalString("notes").orEmpty()
        val windows = root["windows"]?.jsonObject
            ?: throw IllegalArgumentException("У відповіді оновлень відсутній блок 'windows'.")
        val windowsUrl = windows.requiredString("url")
        val sha256 = windows.optionalString("sha256")

        return UpdateRelease(
            version = version,
            notes = notes,
            windowsUrl = windowsUrl,
            sha256 = sha256
        )
    }

    private suspend fun downloadInstaller(release: UpdateRelease, installerPath: Path) {
        val downloadUri = parseHttpsUri(release.windowsUrl, "URL завантаження MSI")
        val request = HttpRequest.newBuilder(downloadUri)
            .GET()
            .timeout(Duration.ofMinutes(10))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            throw IOException("Не вдалося завантажити оновлення (HTTP ${response.statusCode()}).")
        }

        val totalBytes = response.headers()
            .firstValue("content-length")
            .orElse("")
            .toLongOrNull()
            ?.takeIf { it > 0L }

        _uiState.update { it.copy(totalBytes = totalBytes, downloadedBytes = 0L) }

        response.body().use { input ->
            Files.newOutputStream(
                installerPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            ).use { output ->
                val buffer = ByteArray(64 * 1024)
                var downloadedBytes = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    _uiState.update {
                        it.copy(
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }
            }
        }

        if (installerPath.extension.lowercase() != "msi") {
            throw IllegalStateException("Завантажений файл не є MSI.")
        }
    }

    private fun verifyChecksumIfNeeded(release: UpdateRelease, installerPath: Path) {
        val expected = release.sha256
        if (expected == null) {
            if (AppBuildConfig.ALLOW_UPDATES_WITHOUT_CHECKSUM) {
                println("Warning: SHA-256 is missing for ${release.version}; proceeding due configuration.")
                return
            }
            throw IllegalStateException("Оновлення без SHA-256 заборонені.")
        }

        val actualHash = calculateSha256(installerPath)
        if (!actualHash.equals(expected, ignoreCase = true)) {
            throw IllegalStateException("Контрольна сума MSI не збігається.")
        }
    }

    private fun calculateSha256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun parseHttpsUri(raw: String, fieldName: String): URI {
        val uri = try {
            URI(raw.trim())
        } catch (_: Exception) {
            throw IllegalArgumentException("$fieldName має некоректний формат.")
        }

        if (!uri.isAbsolute) {
            throw IllegalArgumentException("$fieldName має бути абсолютним URL.")
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw IllegalArgumentException("$fieldName повинен використовувати HTTPS.")
        }
        if (uri.host.isNullOrBlank()) {
            throw IllegalArgumentException("$fieldName не містить host.")
        }
        return uri
    }

    private fun resolveAllowedHosts(feedUri: URI): Set<String> {
        return if (configuredAllowedHosts.isNotEmpty()) {
            configuredAllowedHosts
        } else {
            setOf(feedUri.host.lowercase())
        }
    }

    private fun ensureHostAllowed(uri: URI, allowedHosts: Set<String>) {
        val host = uri.host?.lowercase()
            ?: throw IllegalArgumentException("URL не містить host.")
        if (host !in allowedHosts) {
            throw IllegalArgumentException("Host '$host' не входить до allowlist оновлень.")
        }
    }

    private fun resolveInstallerPath(version: String): Path {
        val safeVersion = version.replace(Regex("[^0-9A-Za-z._-]"), "_")
        return updatesDirectory.resolve("${AppBuildConfig.APP_NAME}-$safeVersion.msi")
    }

    private fun cleanupOldInstallers(keepFileName: String) {
        if (!Files.exists(updatesDirectory)) return

        Files.list(updatesDirectory).use { stream ->
            stream
                .filter { path ->
                    path.isRegularFile() &&
                        path.extension.lowercase() == "msi" &&
                        path.name != keepFileName
                }
                .forEach { path -> path.deleteIfExists() }
        }
    }

    private fun buildChecksumWarning(release: UpdateRelease): String? {
        if (release.sha256 != null) return null
        return if (AppBuildConfig.ALLOW_UPDATES_WITHOUT_CHECKSUM) {
            "Оновлення без SHA-256 дозволені. Рекомендується додати контрольну суму."
        } else {
            "Оновлення без SHA-256 заблоковані політикою безпеки."
        }
    }

    private fun parseSemVerOrThrow(value: String): SemVer = try {
        SemVer.parse(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Версія '$value' не відповідає SemVer.")
    }

    private fun isNewerVersion(candidate: String, current: String): Boolean {
        val candidateSemVer = parseSemVerOrThrow(candidate)
        val currentSemVer = parseSemVerOrThrow(current)
        return candidateSemVer > currentSemVer
    }

    private fun validateSha256(hash: String) {
        val normalized = hash.trim()
        if (!normalized.matches(Regex("^[A-Fa-f0-9]{64}$"))) {
            throw IllegalArgumentException("SHA-256 має містити 64 hex-символи.")
        }
    }

    private fun toUserMessage(error: Exception): String {
        return when (error) {
            is SSLException -> "Неможливо встановити захищене HTTPS-з'єднання для оновлення."
            is java.net.http.HttpTimeoutException -> "Сервер оновлень не відповідає (таймаут)."
            is java.net.UnknownHostException -> "Не вдалося знайти сервер оновлень. Перевірте інтернет."
            is IOException -> error.message ?: "Помилка завантаження оновлення."
            is IllegalArgumentException -> error.message ?: "Отримано некоректні дані оновлення."
            else -> error.message ?: "Сталася невідома помилка оновлення."
        }
    }
}

private fun JsonObject.requiredString(key: String): String {
    val value = this[key]?.jsonPrimitive?.contentOrNull?.trim()
    if (value.isNullOrEmpty()) {
        throw IllegalArgumentException("У feed відсутнє поле '$key'.")
    }
    return value
}

private fun JsonObject.optionalString(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}
