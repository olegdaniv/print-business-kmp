# PrintBusinessKmp — Domain Models Refactoring Specification

> **Purpose:** This document is a complete refactoring guide for the shared module's domain models.  
> **Target:** `shared/src/commonMain/kotlin/com/printbusinesskmp/models/`  
> **Context:** The business is expanding from basic thermal printing to a full DTF (Direct-to-Film) + UV DTF printing service with outsourced printing, product catalog, and delivery tracking.  
> **Approach:** Incremental migration — all new fields have default values to maintain backward compatibility. Existing API endpoints and UI screens should continue working during migration.

---

## Table of Contents

1. [Business Context](#1-business-context)
2. [Migration Summary](#2-migration-summary)
3. [Phase 1: Enums — Extend Existing + Add New](#3-phase-1-enums)
4. [Phase 2: Extend Existing Models](#4-phase-2-extend-existing-models)
5. [Phase 3: New Models](#5-phase-3-new-models)
6. [Phase 4: Backend Routes & DB Schema](#6-phase-4-backend-routes--db-schema)
7. [Phase 5: PricingCalculator Update](#7-phase-5-pricingcalculator-update)
8. [Backward Compatibility Notes](#8-backward-compatibility-notes)

---

## 1. Business Context

The business model is **outsource-first**: we do NOT print DTF/UV DTF ourselves. Instead:

1. **Client** places an order (custom print on their garment, or ready-made product from our catalog)
2. **We** prepare the design and order DTF transfers / UV DTF stickers from a **Partner** (print shop)
3. **We** receive the transfers, apply them via heat press (DTF) or deliver stickers (UV DTF)
4. **We** deliver the finished product to the client

**Three services offered:**
- **DTF on client's garment** — client brings their t-shirt/hoodie, we apply their design
- **Ready-made products with print** — we buy blank garments, apply print, sell as finished goods
- **UV DTF stickers** — stickers for hard surfaces (mugs, phone cases, bottles, etc.)

**Key domain concepts:**
- `GarmentSource` — tracks whether the garment is client-provided, from our stock, or needs purchasing
- `OutsourceOrder` — replaces `OutsourceJob`, tracks orders placed with printing partners
- `Product` — catalog of blank garments and souvenirs we stock
- `Design` — replaces `Layout`, adds categories (custom/catalog/template) and client association
- `Delivery` — shipping via Nova Poshta / Ukrposhta / courier / pickup

---

## 2. Migration Summary

| File | Status | Changes |
|------|--------|---------|
| `ServiceType.kt` | 🔄 EXTEND | +2 values |
| `ProductType.kt` | 🔄 EXTEND | +15 values, +helper properties |
| `OrderStatus.kt` | 🔄 REWRITE | 5 → 11 states, +transition logic |
| `Client.kt` | 🔄 EXTEND | +2 fields, +ClientSource enum |
| `Order.kt` | 🔄 EXTEND | +4 fields on Order, +5 fields on OrderItem |
| `Pricing.kt` | 🔄 EXTEND | +3 fields on PricingConfig, +3 fields on PricingResult |
| `Invoice.kt` | 🔄 EXTEND | +2 fields |
| `BusinessProfile.kt` | 🔄 EXTEND | +3 fields, +SocialLinks |
| `Partner.kt` | 🔄 EXTEND | +PartnerType, +PartnerPriceList, +5 fields |
| `OutsourceJob.kt` | 🆕 REPLACE | → `OutsourceOrder.kt` (new file, delete old) |
| `Product.kt` | 🆕 NEW | New entity with CRUD DTOs |
| `Design.kt` | 🆕 NEW | Replaces `Layout.kt` (keep old until migrated) |
| `Delivery.kt` | 🆕 NEW | New entity with CRUD DTOs |
| `GarmentSource.kt` | 🆕 NEW | New enum |
| `ProductCategory.kt` | 🆕 NEW | New enum |
| `DesignCategory.kt` | 🆕 NEW | New enum |
| `DesignStatus.kt` | 🆕 NEW | New enum |
| `OutsourceOrderStatus.kt` | 🆕 NEW | New enum (replaces OutsourceJobStatus) |
| `DeliveryMethod.kt` | 🆕 NEW | New enum |
| `DeliveryStatus.kt` | 🆕 NEW | New enum |
| `PartnerType.kt` | 🆕 NEW | New enum |
| `ClientSource.kt` | 🆕 NEW | New enum |

---

## 3. Phase 1: Enums

### 3.1 Extend `ServiceType.kt`

```kotlin
@Serializable
enum class ServiceType {
    DTF,
    UV_DTF,
    DTF_TRANSFER_ONLY,  // NEW — sell transfer film without applying
    DESIGN_ONLY          // NEW — design preparation only
}
```

### 3.2 Extend `ProductType.kt`

```kotlin
@Serializable
enum class ProductType {
    // ── Textile (DTF) ──
    T_SHIRT,
    HOODIE,
    SWEATSHIRT,        // NEW
    SHOPPER_BAG,       // NEW
    CAP,               // NEW
    APRON,             // NEW
    BACKPACK,          // NEW
    UNIFORM,           // NEW
    OTHER_TEXTILE,     // NEW

    // ── Hard surfaces (UV DTF) ──
    MUG,               // NEW
    THERMOS,           // NEW
    BOTTLE,            // NEW
    PHONE_CASE,        // NEW
    KEYCHAIN,          // NEW
    PEN,               // NEW
    NOTEBOOK,          // NEW
    SIGN,              // NEW
    GIFT_BOX,          // NEW
    OTHER_HARD,        // NEW

    // ── Backward compat ──
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
```

### 3.3 Rewrite `OrderStatus.kt`

**Old:** `NEW, IN_PRODUCTION, READY, COMPLETED, CANCELLED`  
**New:** Full lifecycle for outsource model.

```kotlin
@Serializable
enum class OrderStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    OUTSOURCE_ORDERED,
    OUTSOURCE_RECEIVED,
    IN_PRODUCTION,
    QUALITY_CHECK,
    READY,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    val isCancellable: Boolean
        get() = this in setOf(DRAFT, PENDING_APPROVAL, APPROVED)

    val isEditable: Boolean
        get() = this in setOf(DRAFT, PENDING_APPROVAL)

    val isFinal: Boolean
        get() = this in setOf(COMPLETED, CANCELLED)

    val allowedTransitions: Set<OrderStatus>
        get() = when (this) {
            DRAFT -> setOf(PENDING_APPROVAL, APPROVED, CANCELLED)
            PENDING_APPROVAL -> setOf(APPROVED, CANCELLED)
            APPROVED -> setOf(OUTSOURCE_ORDERED, IN_PRODUCTION, CANCELLED)
            OUTSOURCE_ORDERED -> setOf(OUTSOURCE_RECEIVED, CANCELLED)
            OUTSOURCE_RECEIVED -> setOf(IN_PRODUCTION)
            IN_PRODUCTION -> setOf(QUALITY_CHECK, READY)
            QUALITY_CHECK -> setOf(READY, IN_PRODUCTION)
            READY -> setOf(SHIPPED, COMPLETED)
            SHIPPED -> setOf(COMPLETED)
            COMPLETED -> emptySet()
            CANCELLED -> emptySet()
        }
}
```

**DB migration:** Rename `NEW` → `DRAFT` in existing records.

### 3.4 New Enum Files (create each as a separate .kt file)

```kotlin
// GarmentSource.kt
@Serializable
enum class GarmentSource {
    CLIENT_PROVIDED,  // Client brings their own garment
    OUR_STOCK,        // From our inventory
    TO_PURCHASE       // Needs to be purchased
}

// ProductCategory.kt
@Serializable
enum class ProductCategory {
    GARMENT,    // Clothing & textile
    SOUVENIR,   // Mugs, keychains, etc.
    ACCESSORY,  // Bags, backpacks, caps
    PACKAGING   // Boxes, promotional materials
}

// DesignCategory.kt
@Serializable
enum class DesignCategory {
    CUSTOM,    // Custom design for specific client
    CATALOG,   // Our catalog design for sale
    TEMPLATE   // Reusable template
}

// DesignStatus.kt
@Serializable
enum class DesignStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    IN_PRODUCTION,
    ARCHIVED
}

// OutsourceOrderStatus.kt
@Serializable
enum class OutsourceOrderStatus {
    PENDING,
    SENT,
    IN_PRODUCTION,
    READY,
    RECEIVED,
    QUALITY_CHECK,
    ACCEPTED,
    REJECTED
}

// DeliveryMethod.kt
@Serializable
enum class DeliveryMethod {
    PICKUP,
    NOVA_POSHTA,
    UKRPOSHTA,
    COURIER
}

// DeliveryStatus.kt
@Serializable
enum class DeliveryStatus {
    PENDING,
    SHIPPED,
    DELIVERED,
    RETURNED
}

// PartnerType.kt
@Serializable
enum class PartnerType {
    DTF_PRINTER,
    UV_DTF_PRINTER,
    BLANK_SUPPLIER,
    SOUVENIR_SUPPLIER,
    DESIGNER,
    OTHER
}

// ClientSource.kt
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
```

---

## 4. Phase 2: Extend Existing Models

### 4.1 `Client.kt` — add fields

Add to `Client` data class:
```kotlin
val source: ClientSource? = null,           // NEW
val discountPercent: Double? = null,        // NEW — individual discount for B2B/loyal clients
```

Add to `ClientCreateRequest` and `ClientUpdateRequest`:
```kotlin
val source: ClientSource? = null,
val discountPercent: Double? = null,
```

### 4.2 `Order.kt` — add fields to Order

Add to `Order` data class:
```kotlin
val discountAmount: Double = 0.0,                   // NEW
val outsourceOrderIds: List<String> = emptyList(),   // NEW — linked outsource orders
val deliveryMethod: DeliveryMethod? = null,          // NEW
val deliveryId: String? = null,                      // NEW
```

### 4.3 `Order.kt` — add fields to OrderItem

Add to `OrderItem` data class:
```kotlin
val garmentSource: GarmentSource = GarmentSource.OUR_STOCK,  // NEW
val size: String? = null,               // NEW — garment size (S, M, L, XL)
val color: String? = null,              // NEW — garment color
val printCost: Double = 0.0,            // NEW — cost of printing at partner
val laborCost: Double = 0.0,            // NEW — cost of transfer/cutting work
val designId: String? = null,           // NEW — link to Design entity
val outsourceOrderId: String? = null,   // NEW — link to OutsourceOrder
```

Add same fields to `OrderItemDraft`, `OrderCreateRequest`/`OrderItemCreateRequest`.

### 4.4 `Pricing.kt` — extend PricingConfig

Add to `PricingConfig`:
```kotlin
val partnerPrintCostFlat: Double = 0.0,  // NEW — flat cost from partner (UV DTF or small formats)
val laborCostPerUnit: Double = 0.0,      // NEW — labor cost per unit (transfer, cutting)
```

Add to `PricingResult`:
```kotlin
val partnerPrintCost: Double = 0.0,      // NEW — partner printing cost
val laborCost: Double = 0.0,             // NEW — labor cost
val actualMarginPercent: Double = 0.0,   // NEW — actual realized margin %
```

Add to `PricingRequest`:
```kotlin
val garmentSource: GarmentSource = GarmentSource.OUR_STOCK,  // NEW
val partnerPrintCostFlat: Double = 0.0,                       // NEW
val laborCostPerUnit: Double = 0.0,                           // NEW
val serviceType: ServiceType = ServiceType.DTF,               // NEW
val productType: ProductType = ProductType.T_SHIRT,           // NEW
```

### 4.5 `Invoice.kt` — minor additions

Add to `Invoice`:
```kotlin
val discountAmount: Double = 0.0,  // NEW
val finalAmount: Double = 0.0,     // NEW — totalAmount - discountAmount
```

### 4.6 `BusinessProfile.kt` — brand info

Add to `BusinessProfile`:
```kotlin
val monthlyEsv: Double = 1900.0,        // NEW — monthly social contribution
val brandName: String? = null,           // NEW — brand name for website
val logoUrl: String? = null,             // NEW
val socialLinks: SocialLinks? = null,    // NEW
```

Create `SocialLinks` in same file:
```kotlin
@Serializable
data class SocialLinks(
    val instagram: String? = null,
    val tiktok: String? = null,
    val facebook: String? = null,
    val telegram: String? = null,
    val website: String? = null,
)
```

### 4.7 `Partner.kt` — extend significantly

Add to `Partner`:
```kotlin
val type: PartnerType = PartnerType.DTF_PRINTER,  // NEW
val website: String? = null,                        // NEW
val priceList: PartnerPriceList? = null,            // NEW
val minimumOrder: String? = null,                   // NEW
val avgLeadTimeDays: Int? = null,                   // NEW
val qualityRating: Double? = null,                  // NEW — 1.0 to 5.0
val isActive: Boolean = true,                       // NEW
```

Create `PartnerPriceList` in same file:
```kotlin
@Serializable
data class PartnerPriceList(
    val dtfPricePerMeter: Double? = null,
    val uvDtfPricePerSheet: Double? = null,
    val uvDtfSheetSize: String? = null,
    val uvDtfPricePerSqMeter: Double? = null,
    val customItems: Map<String, Double> = emptyMap(),
    val updatedAt: kotlin.time.Instant? = null,
)
```

---

## 5. Phase 3: New Models

### 5.1 `Product.kt` — NEW FILE

```kotlin
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
    val createdAt: kotlin.time.Instant? = null,
    val updatedAt: kotlin.time.Instant? = null,
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
```

**Backend:** Add CRUD routes `/api/products` and DB table `products`.

### 5.2 `Design.kt` — NEW FILE (replaces Layout)

```kotlin
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
    val createdAt: kotlin.time.Instant? = null,
    val updatedAt: kotlin.time.Instant? = null,
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
```

**Migration from Layout:**  
- Map `Layout.name` → `Design.name`  
- Map `Layout.serviceType` → `Design.serviceType`  
- Map `Layout.widthCm/heightCm/dpi` → `Design.widthCm/heightCm/dpiResolution`  
- Map `Layout.previewUrl` → `Design.previewUrl`  
- Map `LayoutStatus` → `DesignStatus` (FUTURE→DRAFT, IN_PROGRESS→DRAFT, READY→APPROVED, PRINTED→IN_PRODUCTION, ARCHIVED→ARCHIVED)  
- Set `Design.category = CATALOG` for all migrated layouts  
- Keep `Layout.kt` until all UI screens are migrated, then delete  
- Rename backend routes: `/api/layouts` → `/api/designs`  

### 5.3 `OutsourceOrder.kt` — NEW FILE (replaces OutsourceJob)

```kotlin
@Serializable
data class OutsourceOrder(
    val id: String = "",
    val partnerId: String = "",
    val partnerName: String? = null,
    val orderIds: List<String> = emptyList(),
    val serviceType: ServiceType = ServiceType.DTF,
    val description: String = "",
    val specifications: String? = null,
    val totalArea: Double = 0.0,
    val areaUnit: String = "м²",
    val status: OutsourceOrderStatus = OutsourceOrderStatus.PENDING,
    val costFromPartner: Double = 0.0,
    val orderedAt: kotlin.time.Instant? = null,
    val expectedDelivery: kotlin.time.Instant? = null,
    val receivedAt: kotlin.time.Instant? = null,
    val qualityNotes: String? = null,
    val createdAt: kotlin.time.Instant? = null,
    val updatedAt: kotlin.time.Instant? = null,
)

@Serializable
data class OutsourceOrderCreateRequest(
    val partnerId: String,
    val orderIds: List<String> = emptyList(),
    val serviceType: ServiceType = ServiceType.DTF,
    val description: String,
    val specifications: String? = null,
    val totalArea: Double = 0.0,
    val areaUnit: String = "м²",
    val costFromPartner: Double = 0.0,
    val expectedDelivery: kotlin.time.Instant? = null,
)
```

**Migration from OutsourceJob:**  
- Map `OutsourceJob.orderId` → `OutsourceOrder.orderIds` (single → list)  
- Map `OutsourceJob.costToYou` → `OutsourceOrder.costFromPartner`  
- Map `OutsourceJobStatus` → `OutsourceOrderStatus`  
- Delete `OutsourceJob.kt` and `OutsourceJobStatus.kt` after migration  

### 5.4 `Delivery.kt` — NEW FILE

```kotlin
@Serializable
data class Delivery(
    val id: String = "",
    val orderId: String = "",
    val method: DeliveryMethod = DeliveryMethod.PICKUP,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val trackingNumber: String? = null,
    val cost: Double = 0.0,
    val paidByClient: Boolean = true,
    val address: String? = null,
    val city: String? = null,
    val warehouseNumber: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val shippedAt: kotlin.time.Instant? = null,
    val deliveredAt: kotlin.time.Instant? = null,
    val notes: String? = null,
    val createdAt: kotlin.time.Instant? = null,
)

@Serializable
data class DeliveryCreateRequest(
    val orderId: String,
    val method: DeliveryMethod = DeliveryMethod.PICKUP,
    val trackingNumber: String? = null,
    val cost: Double = 0.0,
    val paidByClient: Boolean = true,
    val address: String? = null,
    val city: String? = null,
    val warehouseNumber: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val notes: String? = null,
)
```

**Backend:** Add CRUD routes `/api/deliveries` and DB table `deliveries`.

---

## 6. Phase 4: Backend Routes & DB Schema

### New API Routes to Add

| Route | Methods | Entity |
|-------|---------|--------|
| `/api/products` | GET, POST | Product |
| `/api/products/{id}` | GET, PUT, DELETE | Product |
| `/api/designs` | GET, POST | Design (replaces /api/layouts) |
| `/api/designs/{id}` | GET, PUT, DELETE | Design |
| `/api/outsource-orders` | GET, POST | OutsourceOrder (replaces outsource-jobs) |
| `/api/outsource-orders/{id}` | GET, PUT, DELETE | OutsourceOrder |
| `/api/outsource-orders/{id}/status` | PUT | Status update |
| `/api/deliveries` | GET, POST | Delivery |
| `/api/deliveries/{id}` | GET, PUT | Delivery |
| `/api/orders/{id}/status` | PUT | Order status transition (validate via allowedTransitions) |

### DB Schema Changes

Add new tables: `products`, `designs`, `outsource_orders`, `deliveries`.  
Alter existing tables: add new columns with defaults (see Phase 2 field additions).  
Rename: `layouts` → `designs`, `outsource_jobs` → `outsource_orders`.

---

## 7. Phase 5: PricingCalculator Update

Update `utils/PricingCalculator.kt` to handle the new cost structure:

```
Total Cost = garmentCost (if OUR_STOCK or TO_PURCHASE)
           + partnerPrintCost (DTF per meter OR UV DTF flat)
           + laborCost (transfer/cutting work)
           + overheadCost
           + wasteCost

Suggested Price = totalCost × (1 + marginPercent)
Final Price = max(suggestedPrice, minOrderPrice)
Tax Amount = finalPrice × taxPercent  (NOTE: ФОП 5% is on REVENUE, not profit)
Profit = finalPrice - totalCost - taxAmount
Actual Margin = profit / finalPrice × 100
```

Key changes:
- When `garmentSource == CLIENT_PROVIDED`, set `garmentCost = 0`
- When `serviceType == UV_DTF`, use `partnerPrintCostFlat` instead of meters-based calculation
- Add `laborCostPerUnit` to cost breakdown
- Calculate and return `actualMarginPercent`

---

## 8. Backward Compatibility Notes

### All new fields have defaults
Every new field added to existing models has a default value, so existing serialized data will deserialize without errors.

### OrderStatus migration
Existing records with `NEW` status must be renamed to `DRAFT` in the database. Add a DB migration script.

### Layout → Design coexistence
Keep both `Layout.kt` and `Design.kt` during transition. The `LayoutsScreen` can continue using `Layout` while `DesignsScreen` is built. Delete `Layout.kt` only after all references are migrated.

### OutsourceJob → OutsourceOrder
Same approach — keep both during transition, then delete the old one.

### API versioning
No API versioning needed — new endpoints are additive, existing endpoints continue to work.

### Recommended execution order
1. Add all new enum files (zero risk, no existing code affected)
2. Extend existing enums (ServiceType, ProductType) — update DisplayLabels
3. Rewrite OrderStatus + DB migration for `NEW` → `DRAFT`
4. Add new fields to existing models (Client, Order, OrderItem, Pricing, etc.)
5. Create Product entity + routes + screen
6. Create Design entity + routes + screen, then migrate Layout data
7. Create OutsourceOrder entity + routes + screen, then migrate OutsourceJob data
8. Create Delivery entity + routes + screen
9. Update PricingCalculator
10. Update OrderFormScreen for new fields (garmentSource, size, color, designId)
11. Update DashboardScreen for new metrics
12. Delete deprecated files (Layout.kt, OutsourceJob.kt, LayoutsScreen.kt)
