package com.flowpay.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic accent colors for FlowPay. Used so Compose UI can switch between Theme Blue and Theme Red.
 */
data class FlowPayAccentTheme(
    val primary: Color,
    val primaryDark: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val accent: Color,
    val accentLight: Color
)

// Theme Blue: current app colors
val BlueAccentTheme = FlowPayAccentTheme(
    primary = Color(0xFF5B8DEF),
    primaryDark = Color(0xFF1976D2),
    headerGradientStart = Color(0xFF7BA8F5),
    headerGradientEnd = Color(0xFF6A96EE),
    accent = Color(0xFF4A90E2),
    accentLight = Color(0xFF4A9EFF)
)

// Theme Red: deep ruby-garnet palette — premium dark red with visual presence
val RedAccentTheme = FlowPayAccentTheme(
    primary             = Color(0xFF9B1B30),  // deep ruby — card surfaces & primary elements
    primaryDark         = Color(0xFF6B0F1F),  // dark garnet — deepest surfaces
    headerGradientStart = Color(0xFFB82040),  // rich ruby (L~43%) — top of button/header gradient
    headerGradientEnd   = Color(0xFF8B1528),  // dark garnet (L~32%) — bottom, creates visible depth
    accent              = Color(0xFFC42845),  // brighter ruby (L~47%) — interactive elements, icons
    accentLight         = Color(0xFFD63558)   // lighter ruby — borders, pressed states
)

val LocalFlowPayAccentTheme = compositionLocalOf { BlueAccentTheme }
