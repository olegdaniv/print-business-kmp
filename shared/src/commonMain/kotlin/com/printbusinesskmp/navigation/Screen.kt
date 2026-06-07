package com.printbusinesskmp.navigation

sealed class Screen {
    data object Dashboard : Screen()
    data object BusinessProfile : Screen()
    data object Clients : Screen()
    data class ClientForm(val clientId: String? = null) : Screen()
    data object Orders : Screen()
    data class OrderForm(val orderId: String? = null) : Screen()
    data class OrderDetail(val orderId: String) : Screen()
    data object Layouts : Screen()
    data object Invoices : Screen()
    data object Updates : Screen()
    data object Settings : Screen()
}
