package com.printbusinesskmp.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Centralized typography scale for PrintBusinessKmp application.
 * All font sizes used throughout the application are defined here.
 * Follows a consistent typographic scale (12, 14, 16, 18, 20, 24, 28, 32sp).
 */
object AppTypography {

    // Heading Sizes
    object Heading {
        val H1: TextUnit = 32.sp     // Page title heading (largest)
        val H2: TextUnit = 28.sp     // Large stat card value
        val H3: TextUnit = 24.sp     // Section title
        val H4: TextUnit = 20.sp     // Dialog/card title
        val H5: TextUnit = 18.sp     // Emphasis text
    }

    // Body Text Sizes
    object Body {
        val Large: TextUnit = 16.sp    // Primary body text, sidebar items, button text
        val Medium: TextUnit = 14.sp   // Secondary text, labels, form fields
        val Small: TextUnit = 12.sp    // Small text, badges, hints
    }

    // Convenience aliases for common usage
    val PageTitle: TextUnit = Heading.H1
    val SectionTitle: TextUnit = Heading.H3
    val CardTitle: TextUnit = Heading.H4
    val BodyText: TextUnit = Body.Large
    val Label: TextUnit = Body.Medium
    val Caption: TextUnit = Body.Small
}