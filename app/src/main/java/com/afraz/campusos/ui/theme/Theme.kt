package com.afraz.campusos.ui.theme

import android.animation.ValueAnimator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun CampusOsTheme(palette: CampusColorPalette = CampusPalettes.default, content: @Composable () -> Unit) {
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accentPrimary,
            onPrimary = palette.heroCardText,
            secondary = palette.accentSecondary,
            onSecondary = palette.heroBadgeText,
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.surfacePrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            error = palette.statusDanger,
            outline = palette.borderSubtle,
            secondaryContainer = palette.accentPrimary.copy(alpha = 0.22f),
            onSecondaryContainer = palette.textPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.accentPrimary,
            onPrimary = palette.heroCardText,
            secondary = palette.accentSecondary,
            onSecondary = palette.heroBadgeText,
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.surfacePrimary,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            error = palette.statusDanger,
            outline = palette.borderSubtle,
            secondaryContainer = palette.accentPrimary.copy(alpha = 0.14f),
            onSecondaryContainer = palette.textPrimary
        )
    }
    CompositionLocalProvider(
        LocalMotionEnabled provides ValueAnimator.areAnimatorsEnabled(),
        LocalCampusColors provides palette
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = CampusTypography, shapes = CampusShapes, content = content)
    }
}

