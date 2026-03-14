package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Design(
    val id: String = "",
    val name: String = "",
    val serviceType: ServiceType = ServiceType.DTF,
    val category: DesignCategory = DesignCategory.CUSTOM,
    val status: DesignStatus = DesignStatus.DRAFT,
    val widthCm: Double = 0.0,
    val heightCm: Double = 0.0,
    val dpiResolution: Int = 300,
    val fileUrl: String? = null,
    val previewUrl: String? = null,
    val clientId: String? = null,
    val tags: List<String> = emptyList(),
    val usageCount: Int = 0,
    val notes: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class DesignCreateRequest(
    val name: String,
    val serviceType: ServiceType = ServiceType.DTF,
    val category: DesignCategory = DesignCategory.CUSTOM,
    val widthCm: Double = 0.0,
    val heightCm: Double = 0.0,
    val dpiResolution: Int = 300,
    val fileUrl: String? = null,
    val previewUrl: String? = null,
    val clientId: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class DesignUpdateRequest(
    val name: String? = null,
    val serviceType: ServiceType? = null,
    val category: DesignCategory? = null,
    val status: DesignStatus? = null,
    val widthCm: Double? = null,
    val heightCm: Double? = null,
    val dpiResolution: Int? = null,
    val fileUrl: String? = null,
    val previewUrl: String? = null,
    val clientId: String? = null,
    val tags: List<String>? = null,
    val notes: String? = null,
)
