package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ServiceType {
    DTF,
    UV_DTF
}

@Serializable
enum class ProductType {
    T_SHIRT,
    HOODIE,
    OTHER
}
