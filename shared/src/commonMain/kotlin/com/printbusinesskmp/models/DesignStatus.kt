package com.printbusinesskmp.models

import kotlinx.serialization.Serializable

@Serializable
enum class DesignStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    IN_PRODUCTION,
    ARCHIVED
}
