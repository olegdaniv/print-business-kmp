package com.printbusinesskmp.utils

/**
 * Validation error keys that map to string resources.
 * These keys should be resolved to localized strings in Composable functions.
 */
object ValidationErrorKeys {
    const val NAME_REQUIRED = "validation_name_required"
    const val PHONE_REQUIRED = "validation_phone_required"
    const val PHONE_FORMAT = "validation_phone_format"
    const val EMAIL_FORMAT = "validation_email_format"
}

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

    /**
     * Validates client form fields and returns a map of field names to error keys.
     * Error keys should be resolved to localized strings using stringResource in Composables.
     *
     * @return Map of field names to error resource keys (e.g., "name" to "validation_name_required")
     */
    fun validateClientForm(name: String, phone: String, email: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (!isRequired(name)) {
            errors["name"] = ValidationErrorKeys.NAME_REQUIRED
        }

        if (!isRequired(phone)) {
            errors["phone"] = ValidationErrorKeys.PHONE_REQUIRED
        } else if (!isValidPhone(phone)) {
            errors["phone"] = ValidationErrorKeys.PHONE_FORMAT
        }

        if (email.isNotEmpty() && !isValidEmail(email)) {
            errors["email"] = ValidationErrorKeys.EMAIL_FORMAT
        }

        return errors
    }
}