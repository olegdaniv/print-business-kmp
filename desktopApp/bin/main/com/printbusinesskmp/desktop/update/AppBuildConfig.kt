package com.printbusinesskmp.desktop.update

internal object AppBuildConfig {
    val APP_NAME: String = readProperty(
        key = "printbusiness.app.name",
        defaultValue = "PrintBusiness"
    )
    val VERSION: String = readProperty(
        key = "printbusiness.app.version",
        defaultValue = "1.0.0"
    )
    val UPDATE_FEED_URL: String = readProperty(
        key = "printbusiness.update.feedUrl",
        defaultValue = "https://example.com/printbusiness/updates/latest.json"
    )
    val UPDATE_ALLOWED_HOSTS: String = readProperty(
        key = "printbusiness.update.allowedHosts",
        defaultValue = ""
    )
    val ALLOW_UPDATES_WITHOUT_CHECKSUM: Boolean = readProperty(
        key = "printbusiness.update.allowWithoutChecksum",
        defaultValue = "true"
    ).toBoolean()

    private fun readProperty(key: String, defaultValue: String): String {
        return System.getProperty(key)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultValue
    }
}
