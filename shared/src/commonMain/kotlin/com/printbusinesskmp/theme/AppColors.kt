package com.printbusinesskmp.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized color palette for PrintBusinessKmp application.
 * All colors used throughout the application are defined here for consistency.
 */
object AppColors {

    // Primary Brand Colors
    val DarkSlate = Color(0xFF1E293B)       // Main text, headings, sidebar background
    val PrimaryBlue = Color(0xFF3B82F6)     // Primary action, selected state
    val White = Color.White                  // Cards, text on dark backgrounds
    val Transparent = Color.Transparent      // Unselected states

    // Neutral/Background Colors
    val LightGray = Color(0xFFF5F5F5)        // Main content background
    val ButtonGray = Color(0xFFE2E8F0)       // Button backgrounds, inactive states
    val CardItemBg = Color(0xFFF8FAFC)       // Card item backgrounds
    val Slate = Color(0xFF334155)            // Language switcher
    val VeryLightBluGray = Color(0xFFF1F5F9) // Default status badge background

    // Text/Label Colors
    val MediumGray = Color(0xFF64748B)       // Secondary text, labels
    val LightGrayText = Color(0xFFCBD5E1)    // Unselected nav items
    val DarkGrayText = Color(0xFF475569)     // Default status text
    val MediumLightGray = Color(0xFF94A3B8)  // Slider labels

    // Status Badge Colors - Backgrounds
    object StatusBackground {
        val New = Color(0xFFDCFCE7)          // NEW status
        val InProgress = Color(0xFFDEEDFF)   // IN_PROGRESS status
        val Ready = Color(0xFFFEF3C7)        // READY status
        val Completed = Color(0xFFD1FAE5)    // COMPLETED status
        val Cancelled = Color(0xFFFEE2E2)    // CANCELLED status
    }

    // Status Badge Colors - Text
    object StatusText {
        val New = Color(0xFF166534)          // NEW text
        val InProgress = Color(0xFF1E40AF)   // IN_PROGRESS text
        val Ready = Color(0xFF854D0E)        // READY text
        val Completed = Color(0xFF065F46)    // COMPLETED text
        val Cancelled = Color(0xFF991B1B)    // CANCELLED text
    }

    // Semantic Colors
    val Success = Color(0xFF16A34A)          // Profit, positive values
    val Error = Color(0xFFEF4444)            // Delete, negative values, errors
    val ErrorDark = Color(0xFFDC2626)        // Costs in calculator
    val Info = Color(0xFF2563EB)             // Pricing information
}