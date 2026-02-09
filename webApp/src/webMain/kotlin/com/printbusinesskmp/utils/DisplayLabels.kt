package com.printbusinesskmp.utils

import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.ProductType
import com.printbusinesskmp.models.ServiceType
import com.printbusinesskmp.models.LayoutStatus

fun OrderStatus.labelUa(): String = when (this) {
    OrderStatus.NEW -> "Нове"
    OrderStatus.IN_PRODUCTION -> "В роботі"
    OrderStatus.READY -> "Готове"
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
}

fun ProductType.labelUa(): String = when (this) {
    ProductType.T_SHIRT -> "Футболка"
    ProductType.HOODIE -> "Худі"
    ProductType.OTHER -> "Інше"
}

fun LayoutStatus.labelUa(): String = when (this) {
    LayoutStatus.FUTURE -> "Майбутнє"
    LayoutStatus.IN_PROGRESS -> "В роботі"
    LayoutStatus.READY -> "Готово"
    LayoutStatus.PRINTED -> "Надруковано"
    LayoutStatus.ARCHIVED -> "Архів"
}
