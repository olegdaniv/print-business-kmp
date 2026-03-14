package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ServiceType {
    DTF,
    UV_DTF,
    DTF_TRANSFER_ONLY,
    DESIGN_ONLY
}