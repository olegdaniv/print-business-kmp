package com.printbusinesskmp.utils

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return true // Email is optional
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    fun isValidPhone(phone: String): Boolean {
        if (phone.isEmpty()) return false
        // Ukrainian phone format: +380XXXXXXXXX
        val phoneRegex = "^\\+380\\d{9}$".toRegex()
        return phoneRegex.matches(phone.replace(" ", ""))
    }

    fun isRequired(value: String): Boolean {
        return value.isNotBlank()
    }

    fun validateClientForm(name: String, phone: String, email: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (!isRequired(name)) {
            errors["name"] = "Name is required"
        }

        if (!isRequired(phone)) {
            errors["phone"] = "Phone is required"
        } else if (!isValidPhone(phone)) {
            errors["phone"] = "Invalid phone format. Use +380XXXXXXXXX"
        }

        if (email.isNotEmpty() && !isValidEmail(email)) {
            errors["email"] = "Invalid email format"
        }

        return errors
    }
}