package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class LayoutStatus {
    FUTURE,
    IN_PROGRESS,
    READY,
    PRINTED,
    ARCHIVED
}

@Serializable
data class Layout(
    val id: String,
    val clientId: String? = null,
    val name: String,
    val serviceType: ServiceType,
    val status: LayoutStatus,
    val widthCm: Double,
    val heightCm: Double,
    val dpi: Int,
    val previewUrl: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class LayoutCreateRequest(
    val clientId: String? = null,
    val name: String,
    val serviceType: ServiceType,
    val status: LayoutStatus = LayoutStatus.FUTURE,
    val widthCm: Double,
    val heightCm: Double,
    val dpi: Int,
    val previewUrl: String? = null,
    val notes: String? = null
)

@Serializable
data class LayoutUpdateRequest(
    val clientId: String? = null,
    val name: String,
    val serviceType: ServiceType,
    val status: LayoutStatus,
    val widthCm: Double,
    val heightCm: Double,
    val dpi: Int,
    val previewUrl: String? = null,
    val notes: String? = null
)
