package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SavedItem(
    val id: String,
    val name: String,
    val unit: String,
    val defaultPrice: Double,
    val createdAt: Instant
)

@Serializable
data class SavedItemCreateRequest(
    val name: String,
    val unit: String = "шт.",
    val defaultPrice: Double = 0.0
)

@Serializable
data class SavedItemBulkUpsertRequest(
    val items: List<SavedItemCreateRequest>
)
