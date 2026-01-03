package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class PrintArea {
    FRONT,
    BACK,
    BOTH,
    SLEEVE,
    CUSTOM
}