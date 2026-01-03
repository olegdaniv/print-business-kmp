package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    T_SHIRT,
    HOODIE,
    CAP,
    BAG,
    CUSTOM
}