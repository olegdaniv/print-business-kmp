package com.printbusinesskmp.navigation

sealed class Screen {
    data object Dashboard : Screen()
    data object Clients : Screen()
    data class ClientDetail(val clientId: String?) : Screen()
    data object Orders : Screen()
    data class OrderDetail(val orderId: String) : Screen()
    data object NewOrder : Screen()
    data object Invoices : Screen()
    data object PricingCalculator : Screen()
}