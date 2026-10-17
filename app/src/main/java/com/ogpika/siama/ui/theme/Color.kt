package com.ogpika.siama.ui.theme

import androidx.compose.ui.graphics.Color

// Minimal Premium Monochrome & Emerald Palette

// Dark Theme Palette (Obsidian Zinc)
val DarkBackground = Color(0xFF09090B)
val DarkCard = Color(0xFF141417)
val DarkCardElevated = Color(0xFF1F1F23)
val DarkBorder = Color(0xFF27272A)
val DarkBorderSubtle = Color(0xFF1E1E22)
val DarkTextPrimary = Color(0xFFF4F4F5)
val DarkTextSecondary = Color(0xFFA1A1AA)
val DarkTextMuted = Color(0xFF71717A)
val DarkAccent = Color(0xFF10B981) // Crisp Emerald
val DarkAccentHover = Color(0xFF059669)

// Light Theme Palette (Clean Studio White & Platinum)
val LightBackground = Color(0xFFF8FAFC)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightBorderSubtle = Color(0xFFF1F5F9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)
val LightAccent = Color(0xFF059669) // Deep Emerald
val LightAccentHover = Color(0xFF047857)

// Attendance Status Semantics
val StatusPresent = Color(0xFF16A34A) // Crisp Green
val StatusPresentDark = Color(0xFF22C55E)
val StatusPresentBg = Color(0xFF16A34A).copy(alpha = 0.15f)

val StatusAbsent = Color(0xFFDC2626) // Vivid Red
val StatusAbsentDark = Color(0xFFEF4444)
val StatusAbsentBg = Color(0xFFDC2626).copy(alpha = 0.15f)

val StatusLeave = Color(0xFFD97706) // Warm Amber
val StatusLeaveDark = Color(0xFFF59E0B)
val StatusLeaveBg = Color(0xFFD97706).copy(alpha = 0.15f)

val StatusHoliday = Color(0xFF4F46E5) // Clean Indigo
val StatusHolidayDark = Color(0xFF818CF8)
val StatusHolidayBg = Color(0xFF4F46E5).copy(alpha = 0.15f)