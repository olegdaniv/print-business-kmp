package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ClientSource {
    WEBSITE,
    INSTAGRAM,
    TIKTOK,
    REFERRAL,
    WALK_IN,
    B2B_OUTREACH,
    OTHER
}
