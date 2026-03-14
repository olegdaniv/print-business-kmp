package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String = "",
    val name: String = "",
    val category: ProductCategory = ProductCategory.GARMENT,
    val productType: ProductType = ProductType.T_SHIRT,
    val serviceType: ServiceType = ServiceType.DTF,
    val supplier: String? = null,
    val purchasePrice: Double = 0.0,
    val availableSizes: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val stockQuantity: Int = 0,
    val minStockLevel: Int = 0,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class ProductCreateRequest(
    val name: String,
    val category: ProductCategory = ProductCategory.GARMENT,
    val productType: ProductType = ProductType.T_SHIRT,
    val serviceType: ServiceType = ServiceType.DTF,
    val supplier: String? = null,
    val purchasePrice: Double = 0.0,
    val availableSizes: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val stockQuantity: Int = 0,
    val minStockLevel: Int = 0,
    val imageUrl: String? = null,
    val notes: String? = null,
)

@Serializable
data class ProductUpdateRequest(
    val name: String? = null,
    val category: ProductCategory? = null,
    val productType: ProductType? = null,
    val serviceType: ServiceType? = null,
    val supplier: String? = null,
    val purchasePrice: Double? = null,
    val availableSizes: List<String>? = null,
    val availableColors: List<String>? = null,
    val stockQuantity: Int? = null,
    val minStockLevel: Int? = null,
    val imageUrl: String? = null,
    val isActive: Boolean? = null,
    val notes: String? = null,
)
