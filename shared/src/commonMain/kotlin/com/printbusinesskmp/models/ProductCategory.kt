package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ProductCategory {
    GARMENT,
    SOUVENIR,
    ACCESSORY,
    PACKAGING
}
