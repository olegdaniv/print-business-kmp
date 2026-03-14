package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    // Textile (DTF)
    T_SHIRT,
    HOODIE,
    SWEATSHIRT,
    SHOPPER_BAG,
    CAP,
    APRON,
    BACKPACK,
    UNIFORM,
    OTHER_TEXTILE,

    // Hard surfaces (UV DTF)
    MUG,
    THERMOS,
    BOTTLE,
    PHONE_CASE,
    KEYCHAIN,
    PEN,
    NOTEBOOK,
    SIGN,
    GIFT_BOX,
    OTHER_HARD,

    // Backward compat
    OTHER;

    val isTextile: Boolean
        get() = this in TEXTILE_TYPES

    val isHardSurface: Boolean
        get() = this in HARD_SURFACE_TYPES

    val defaultServiceType: ServiceType
        get() = when {
            isTextile -> ServiceType.DTF
            isHardSurface -> ServiceType.UV_DTF
            else -> ServiceType.DTF
        }

    companion object {
        val TEXTILE_TYPES = setOf(
            T_SHIRT, HOODIE, SWEATSHIRT, SHOPPER_BAG, CAP,
            APRON, BACKPACK, UNIFORM, OTHER_TEXTILE
        )
        val HARD_SURFACE_TYPES = setOf(
            MUG, THERMOS, BOTTLE, PHONE_CASE, KEYCHAIN,
            PEN, NOTEBOOK, SIGN, GIFT_BOX, OTHER_HARD
        )
    }
}