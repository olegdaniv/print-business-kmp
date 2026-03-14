package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class GarmentSource {
    CLIENT_PROVIDED,
    OUR_STOCK,
    TO_PURCHASE
}
