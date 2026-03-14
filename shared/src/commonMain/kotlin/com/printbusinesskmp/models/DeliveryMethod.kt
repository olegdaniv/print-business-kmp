package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class DeliveryMethod {
    PICKUP,
    NOVA_POSHTA,
    UKRPOSHTA,
    COURIER
}
