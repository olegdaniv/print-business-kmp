package com.printbusinesskmp.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized dimension values for PrintBusinessKmp application.
 * All spacing, sizing, and dimension values used throughout the application.
 * Follows Material Design spacing system (4dp increments).
 */
object AppDimensions {

    // Standard Padding/Spacing Values
    object Padding {
        val ExtraSmall: Dp = 4.dp   // Minimal spacing
        val Small: Dp = 8.dp        // Extra small spacing
        val Medium: Dp = 12.dp      // Small spacing
        val Standard: Dp = 16.dp    // Standard spacing
        val Large: Dp = 20.dp       // Medium-large spacing
        val ExtraLarge: Dp = 24.dp  // Large spacing
    }

    // Component-Specific Dimensions
    object Component {
        // Sidebar
        val SidebarWidth: Dp = 250.dp
        val SidebarItemHeight: Dp = 48.dp

        // Buttons
        val ButtonHeight: Dp = 48.dp

        // Text Fields
        val TextAreaHeight: Dp = 120.dp

        // Language Switcher
        val LanguageSwitcherHeight: Dp = 40.dp

        // Icons
        val IconMedium: Dp = 20.dp
        val IconLarge: Dp = 24.dp
    }

    // Card & Container Dimensions
    object Card {
        val Padding: Dp = 16.dp
        val PaddingLarge: Dp = 20.dp
        val Elevation: Dp = 2.dp
    }

    // Badge Dimensions
    object Badge {
        val PaddingVertical: Dp = 4.dp
        val PaddingHorizontal: Dp = 8.dp
    }

    // Spacing Between Elements (for spacedBy)
    object Spacing {
        val Small: Dp = 8.dp        // Small gaps between inline elements
        val Medium: Dp = 12.dp      // Medium-small gaps (form fields)
        val Standard: Dp = 16.dp    // Standard gaps (order items, stat cards)
        val Large: Dp = 24.dp       // Large gaps (between sections)
    }

    // Form Field Dimensions
    object Form {
        val LabelSpacing: Dp = 12.dp
        val FieldSpacing: Dp = 16.dp
    }
}