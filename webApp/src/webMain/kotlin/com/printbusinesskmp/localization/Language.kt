package com.printbusinesskmp.localization

enum class Language(val code: String, val displayName: String) {
    UKRAINIAN("uk", "Українська"),
    ENGLISH("en", "English");

    companion object {
        val DEFAULT = UKRAINIAN

        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}