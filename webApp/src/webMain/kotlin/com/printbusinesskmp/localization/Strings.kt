package com.printbusinesskmp.localization

object Strings {
    // Common
    fun save(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Зберегти"
        Language.ENGLISH -> "Save"
    }

    fun cancel(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Скасувати"
        Language.ENGLISH -> "Cancel"
    }

    fun add(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Додати"
        Language.ENGLISH -> "Add"
    }

    fun remove(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Видалити"
        Language.ENGLISH -> "Remove"
    }

    fun loading(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Завантаження..."
        Language.ENGLISH -> "Loading..."
    }

    fun error(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Помилка"
        Language.ENGLISH -> "Error"
    }

    fun success(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Успішно"
        Language.ENGLISH -> "Success"
    }

    fun optional(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Необов'язково"
        Language.ENGLISH -> "Optional"
    }

    // Navigation
    fun dashboard(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Панель"
        Language.ENGLISH -> "Dashboard"
    }

    fun clients(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Клієнти"
        Language.ENGLISH -> "Clients"
    }

    fun orders(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Замовлення"
        Language.ENGLISH -> "Orders"
    }

    fun calculator(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Калькулятор"
        Language.ENGLISH -> "Calculator"
    }

    // Order Form Screen
    fun newOrder(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Нове замовлення"
        Language.ENGLISH -> "New Order"
    }

    fun clientSelection(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вибір клієнта"
        Language.ENGLISH -> "Client Selection"
    }

    fun selectClient(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Виберіть клієнта"
        Language.ENGLISH -> "Select a client"
    }

    fun pleaseSelectClient(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Будь ласка, виберіть клієнта"
        Language.ENGLISH -> "Please select a client"
    }

    fun orderItems(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Товари замовлення"
        Language.ENGLISH -> "Order Items"
    }

    fun addItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Додати товар"
        Language.ENGLISH -> "Add Item"
    }

    fun noItemsAdded(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Товари ще не додані"
        Language.ENGLISH -> "No items added yet"
    }

    fun pleaseAddAtLeastOneItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Будь ласка, додайте хоча б один товар"
        Language.ENGLISH -> "Please add at least one item"
    }

    fun quantity(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Кількість"
        Language.ENGLISH -> "Quantity"
    }

    fun qty(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "К-ть"
        Language.ENGLISH -> "Qty"
    }

    fun cost(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вартість"
        Language.ENGLISH -> "Cost"
    }

    fun price(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ціна"
        Language.ENGLISH -> "Price"
    }

    fun profit(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Прибуток"
        Language.ENGLISH -> "Profit"
    }

    fun totalCost(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Загальна вартість"
        Language.ENGLISH -> "Total Cost"
    }

    fun totalPrice(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Загальна ціна"
        Language.ENGLISH -> "Total Price"
    }

    fun totalProfit(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Загальний прибуток"
        Language.ENGLISH -> "Total Profit"
    }

    fun notes(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Примітки"
        Language.ENGLISH -> "Notes"
    }

    fun addNotes(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Додайте будь-які примітки..."
        Language.ENGLISH -> "Add any additional notes..."
    }

    fun saveOrder(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Зберегти замовлення"
        Language.ENGLISH -> "Save Order"
    }

    fun failedToLoadClients(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Не вдалося завантажити клієнтів"
        Language.ENGLISH -> "Failed to load clients"
    }

    fun failedToCreateOrder(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Не вдалося створити замовлення"
        Language.ENGLISH -> "Failed to create order"
    }

    // Add Item Dialog
    fun addOrderItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Додати товар"
        Language.ENGLISH -> "Add Order Item"
    }

    fun productType(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Тип товару"
        Language.ENGLISH -> "Product Type"
    }

    fun size(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Розмір"
        Language.ENGLISH -> "Size"
    }

    fun color(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Колір"
        Language.ENGLISH -> "Color"
    }

    fun printArea(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Область друку"
        Language.ENGLISH -> "Print Area"
    }

    fun designUrl(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "URL дизайну"
        Language.ENGLISH -> "Design URL"
    }

    // Pricing Calculator
    fun pricingCalculator(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Калькулятор цін"
        Language.ENGLISH -> "Pricing Calculator"
    }

    fun laborTime(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Час роботи (хв)"
        Language.ENGLISH -> "Labor Time (min)"
    }

    fun laborRate(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ставка (₴/год)"
        Language.ENGLISH -> "Rate (₴/hour)"
    }

    fun profitMargin(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Маржа прибутку"
        Language.ENGLISH -> "Profit Margin"
    }

    fun calculateCosts(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Розрахувати вартість"
        Language.ENGLISH -> "Calculate Costs"
    }

    fun calculating(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Розрахунок..."
        Language.ENGLISH -> "Calculating..."
    }

    fun pricesCalculatedSuccessfully(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ціни успішно розраховані"
        Language.ENGLISH -> "Prices calculated successfully"
    }

    fun calculationFailed(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Помилка розрахунку"
        Language.ENGLISH -> "Calculation failed"
    }

    fun costBreakdown(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Деталізація вартості"
        Language.ENGLISH -> "Cost Breakdown"
    }

    fun hide(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Сховати"
        Language.ENGLISH -> "Hide"
    }

    fun show(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Показати"
        Language.ENGLISH -> "Show"
    }

    fun totalMaterials(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Матеріали:"
        Language.ENGLISH -> "Total Materials:"
    }

    fun laborCost(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Робота:"
        Language.ENGLISH -> "Labor Cost:"
    }

    fun totalCostPerItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вартість за одиницю:"
        Language.ENGLISH -> "Total Cost per item:"
    }

    fun sellingPricePerItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ціна за одиницю:"
        Language.ENGLISH -> "Selling Price per item:"
    }

    fun tax(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Податок (5%):"
        Language.ENGLISH -> "Tax (5%):"
    }

    fun profitPerItem(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Прибуток за одиницю:"
        Language.ENGLISH -> "Profit per item:"
    }

    fun profitMarginPercent(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Маржа прибутку:"
        Language.ENGLISH -> "Profit Margin:"
    }

    // Manual Cost Entry
    fun manualCostEntry(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ручне введення вартості"
        Language.ENGLISH -> "Manual Cost Entry"
    }

    fun blankItemCost(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вартість заготовки"
        Language.ENGLISH -> "Blank Item Cost"
    }

    fun thermalPaperCost(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вартість термопаперу"
        Language.ENGLISH -> "Thermal Paper Cost"
    }

    fun laborCostManual(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Вартість роботи"
        Language.ENGLISH -> "Labor Cost"
    }

    fun sellingPrice(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Ціна продажу"
        Language.ENGLISH -> "Selling Price"
    }

    // Product Types
    fun productTypeTShirt(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Футболка"
        Language.ENGLISH -> "T-Shirt"
    }

    fun productTypeHoodie(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Худі"
        Language.ENGLISH -> "Hoodie"
    }

    fun productTypeCap(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Кепка"
        Language.ENGLISH -> "Cap"
    }

    fun productTypeBag(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Сумка"
        Language.ENGLISH -> "Bag"
    }

    fun productTypeCustom(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Власний"
        Language.ENGLISH -> "Custom"
    }

    // Print Areas
    fun printAreaFront(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Передня"
        Language.ENGLISH -> "FRONT"
    }

    fun printAreaBack(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Задня"
        Language.ENGLISH -> "BACK"
    }

    fun printAreaBoth(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Обидві"
        Language.ENGLISH -> "BOTH"
    }

    fun printAreaSleeve(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Рукав"
        Language.ENGLISH -> "SLEEVE"
    }

    fun printAreaCustom(lang: Language) = when (lang) {
        Language.UKRAINIAN -> "Власна"
        Language.ENGLISH -> "CUSTOM"
    }

    // Helper function to get product type name
    fun getProductTypeName(productType: com.printbusinesskmp.models.ProductType, lang: Language): String {
        return when (productType) {
            com.printbusinesskmp.models.ProductType.T_SHIRT -> productTypeTShirt(lang)
            com.printbusinesskmp.models.ProductType.HOODIE -> productTypeHoodie(lang)
            com.printbusinesskmp.models.ProductType.CAP -> productTypeCap(lang)
            com.printbusinesskmp.models.ProductType.BAG -> productTypeBag(lang)
            com.printbusinesskmp.models.ProductType.CUSTOM -> productTypeCustom(lang)
        }
    }

    // Helper function to get print area name
    fun getPrintAreaName(printArea: com.printbusinesskmp.models.PrintArea, lang: Language): String {
        return when (printArea) {
            com.printbusinesskmp.models.PrintArea.FRONT -> printAreaFront(lang)
            com.printbusinesskmp.models.PrintArea.BACK -> printAreaBack(lang)
            com.printbusinesskmp.models.PrintArea.BOTH -> printAreaBoth(lang)
            com.printbusinesskmp.models.PrintArea.SLEEVE -> printAreaSleeve(lang)
            com.printbusinesskmp.models.PrintArea.CUSTOM -> printAreaCustom(lang)
        }
    }
}