package com.afraz.campusos.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CampusColorPalette(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val canvas: Color,
    val surfacePrimary: Color,
    val surfaceElevated: Color,
    val navSurface: Color,
    val heroCardBg: Color,
    val heroCardText: Color,
    val heroBadgeBg: Color,
    val heroBadgeText: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderSubtle: Color,
    val statusSafe: Color = Color(0xFF10B981),
    val statusWarning: Color = Color(0xFFF59E0B),
    val statusDanger: Color = Color(0xFFF43F5E)
)

object CampusPalettes {
    val MidnightIndigo = CampusColorPalette(
        id = "midnight-indigo",
        name = "Midnight Indigo",
        isDark = true,
        canvas = Color(0xFF07080C),
        surfacePrimary = Color(0xFF11131C),
        surfaceElevated = Color(0xFF181C28),
        navSurface = Color(0xBF1A1E2E),
        heroCardBg = Color(0xFF1A1E2E),
        heroCardText = Color(0xFFF8FAFC),
        heroBadgeBg = Color(0xFF6366F1),
        heroBadgeText = Color(0xFFF8FAFC),
        accentPrimary = Color(0xFF6366F1),
        accentSecondary = Color(0xFF8B5CF6),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        borderSubtle = Color(0xFF262B3D)
    )

    val CrimsonParchment = CampusColorPalette(
        id = "crimson-parchment",
        name = "Crimson Parchment",
        isDark = false,
        canvas = Color(0xFFF4EEDB),
        surfacePrimary = Color(0xFFEFE6CE),
        surfaceElevated = Color(0xFFFAF5E6),
        navSurface = Color(0xFFEFE6CE),
        heroCardBg = Color(0xFFB3261E),
        heroCardText = Color(0xFFFFF9F2),
        heroBadgeBg = Color(0xFF522316),
        heroBadgeText = Color(0xFFF4EEDB),
        accentPrimary = Color(0xFFB3261E),
        accentSecondary = Color(0xFF522316),
        textPrimary = Color(0xFF2C160E),
        textSecondary = Color(0xFF6B5344),
        borderSubtle = Color(0xFF9C7B63)
    )

    val EspressoParchment = CampusColorPalette(
        id = "espresso-parchment",
        name = "Espresso Parchment",
        isDark = false,
        canvas = Color(0xFFF4EEDB),
        surfacePrimary = Color(0xFFEFE6CE),
        surfaceElevated = Color(0xFFFAF5E6),
        navSurface = Color(0xFFEFE6CE),
        heroCardBg = Color(0xFF4B2114),
        heroCardText = Color(0xFFFFF9F2),
        heroBadgeBg = Color(0xFFB3261E),
        heroBadgeText = Color(0xFFFFF9F2),
        accentPrimary = Color(0xFFB3261E),
        accentSecondary = Color(0xFF4B2114),
        textPrimary = Color(0xFF2C160E),
        textSecondary = Color(0xFF6B5344),
        borderSubtle = Color(0xFF9C7B63)
    )

    val all: List<CampusColorPalette> = listOf(MidnightIndigo, CrimsonParchment, EspressoParchment)
    val default: CampusColorPalette = EspressoParchment
    fun byId(id: String?): CampusColorPalette = all.firstOrNull { it.id == id } ?: default
}

val LocalCampusColors = staticCompositionLocalOf { CampusPalettes.default }

