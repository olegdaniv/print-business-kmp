package com.printbusinesskmp.localization

enum class Language(val code: String, val displayName: String) {
    UKRAINIAN("uk", "Українська"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: UKRAINIAN
        }
    }
}
