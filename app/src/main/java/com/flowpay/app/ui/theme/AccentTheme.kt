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

// Theme Red: vivid crimson palette — bold, modern red with strong presence on dark UI
val RedAccentTheme = FlowPayAccentTheme(
    primary             = Color(0xFFDC3545),  // rich crimson — card surfaces & primary elements
    primaryDark         = Color(0xFF9E1B32),  // deep crimson — darkest surfaces
    headerGradientStart = Color(0xFFEF4056),  // bright rose-crimson — top of button/header gradient
    headerGradientEnd   = Color(0xFFCC2D42),  // mid crimson — bottom, clear gradient depth
    accent              = Color(0xFFFF3B5C),  // vivid rose-red — interactive elements, icons
    accentLight         = Color(0xFFFF5C7A)   // soft rose — borders, pressed states
)

val LocalFlowPayAccentTheme = compositionLocalOf { BlueAccentTheme }
