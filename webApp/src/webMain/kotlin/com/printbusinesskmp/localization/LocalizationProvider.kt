package com.printbusinesskmp.localization

import androidx.compose.runtime.*

object LocalizationState {
    private val _currentLanguage = mutableStateOf(Language.DEFAULT)
    val currentLanguage: State<Language> = _currentLanguage

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }
}