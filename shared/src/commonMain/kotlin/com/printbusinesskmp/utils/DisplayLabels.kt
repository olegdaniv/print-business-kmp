package com.printbusinesskmp.utils

import com.printbusinesskmp.models.*

@Suppress("DEPRECATION")
fun OrderStatus.labelUa(): String = when (this) {
    OrderStatus.DRAFT, OrderStatus.NEW -> "Чернетка"
    OrderStatus.PENDING_APPROVAL -> "Очікує затвердження"
    OrderStatus.APPROVED -> "Затверджено"
    OrderStatus.OUTSOURCE_ORDERED -> "Замовлено у партнера"
    OrderStatus.OUTSOURCE_RECEIVED -> "Отримано від партнера"
    OrderStatus.IN_PRODUCTION -> "В роботі"
    OrderStatus.QUALITY_CHECK -> "Контроль якості"
    OrderStatus.READY -> "Готове"
    OrderStatus.SHIPPED -> "Відправлено"
    OrderStatus.COMPLETED -> "Завершене"
    OrderStatus.CANCELLED -> "Скасоване"
}

fun PaymentStatus.labelUa(): String = when (this) {
    PaymentStatus.UNPAID -> "Не оплачено"
    PaymentStatus.PARTIAL -> "Частково"
    PaymentStatus.PAID -> "Оплачено"
}

fun ServiceType.labelUa(): String = when (this) {
    ServiceType.DTF -> "DTF"
    ServiceType.UV_DTF -> "UV DTF"
    ServiceType.DTF_TRANSFER_ONLY -> "Лише трансфер"
    ServiceType.DESIGN_ONLY -> "Лише дизайн"
}

fun ProductType.labelUa(): String = when (this) {
    ProductType.T_SHIRT -> "Футболка"
    ProductType.HOODIE -> "Худі"
    ProductType.SWEATSHIRT -> "Світшот"
    ProductType.SHOPPER_BAG -> "Шопер"
    ProductType.CAP -> "Кепка"
    ProductType.APRON -> "Фартух"
    ProductType.BACKPACK -> "Рюкзак"
    ProductType.UNIFORM -> "Уніформа"
    ProductType.OTHER_TEXTILE -> "Інший текстиль"
    ProductType.MUG -> "Чашка"
    ProductType.THERMOS -> "Термос"
    ProductType.BOTTLE -> "Пляшка"
    ProductType.PHONE_CASE -> "Чохол для телефону"
    ProductType.KEYCHAIN -> "Брелок"
    ProductType.PEN -> "Ручка"
    ProductType.NOTEBOOK -> "Блокнот"
    ProductType.SIGN -> "Вивіска"
    ProductType.GIFT_BOX -> "Подарункова коробка"
    ProductType.OTHER_HARD -> "Інше (тверде)"
    ProductType.OTHER -> "Інше"
}

fun LayoutStatus.labelUa(): String = when (this) {
    LayoutStatus.FUTURE -> "Майбутнє"
    LayoutStatus.IN_PROGRESS -> "В роботі"
    LayoutStatus.READY -> "Готово"
    LayoutStatus.PRINTED -> "Надруковано"
    LayoutStatus.ARCHIVED -> "Архів"
}

fun GarmentSource.labelUa(): String = when (this) {
    GarmentSource.CLIENT_PROVIDED -> "Виріб клієнта"
    GarmentSource.OUR_STOCK -> "Наш склад"
    GarmentSource.TO_PURCHASE -> "Потрібно закупити"
}

fun ProductCategory.labelUa(): String = when (this) {
    ProductCategory.GARMENT -> "Одяг"
    ProductCategory.SOUVENIR -> "Сувенір"
    ProductCategory.ACCESSORY -> "Аксесуар"
    ProductCategory.PACKAGING -> "Пакування"
}

fun DesignCategory.labelUa(): String = when (this) {
    DesignCategory.CUSTOM -> "Кастомний"
    DesignCategory.CATALOG -> "Каталог"
    DesignCategory.TEMPLATE -> "Шаблон"
}

fun DesignStatus.labelUa(): String = when (this) {
    DesignStatus.DRAFT -> "Чернетка"
    DesignStatus.PENDING_APPROVAL -> "Очікує затвердження"
    DesignStatus.APPROVED -> "Затверджено"
    DesignStatus.IN_PRODUCTION -> "У виробництві"
    DesignStatus.ARCHIVED -> "Архів"
}

fun OutsourceOrderStatus.labelUa(): String = when (this) {
    OutsourceOrderStatus.PENDING -> "Очікує"
    OutsourceOrderStatus.SENT -> "Відправлено"
    OutsourceOrderStatus.IN_PRODUCTION -> "У виробництві"
    OutsourceOrderStatus.READY -> "Готово"
    OutsourceOrderStatus.RECEIVED -> "Отримано"
    OutsourceOrderStatus.QUALITY_CHECK -> "Контроль якості"
    OutsourceOrderStatus.ACCEPTED -> "Прийнято"
    OutsourceOrderStatus.REJECTED -> "Відхилено"
}

fun DeliveryMethod.labelUa(): String = when (this) {
    DeliveryMethod.PICKUP -> "Самовивіз"
    DeliveryMethod.NOVA_POSHTA -> "Нова Пошта"
    DeliveryMethod.UKRPOSHTA -> "Укрпошта"
    DeliveryMethod.COURIER -> "Кур'єр"
}

fun DeliveryStatus.labelUa(): String = when (this) {
    DeliveryStatus.PENDING -> "Очікує"
    DeliveryStatus.SHIPPED -> "Відправлено"
    DeliveryStatus.DELIVERED -> "Доставлено"
    DeliveryStatus.RETURNED -> "Повернуто"
}

fun PartnerType.labelUa(): String = when (this) {
    PartnerType.PERSON -> "Фізична особа"
    PartnerType.COMPANY -> "Компанія"
    PartnerType.DTF_PRINTER -> "DTF друкарня"
    PartnerType.UV_DTF_PRINTER -> "UV DTF друкарня"
    PartnerType.BLANK_SUPPLIER -> "Постачальник заготовок"
    PartnerType.SOUVENIR_SUPPLIER -> "Постачальник сувенірів"
    PartnerType.DESIGNER -> "Дизайнер"
    PartnerType.OTHER -> "Інше"
}

fun ClientSource.labelUa(): String = when (this) {
    ClientSource.WEBSITE -> "Вебсайт"
    ClientSource.INSTAGRAM -> "Instagram"
    ClientSource.TIKTOK -> "TikTok"
    ClientSource.REFERRAL -> "Рекомендація"
    ClientSource.WALK_IN -> "Прямий візит"
    ClientSource.B2B_OUTREACH -> "B2B"
    ClientSource.OTHER -> "Інше"
}
