package com.afraz.campusos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import android.animation.ValueAnimator
import androidx.compose.ui.graphics.Color

val LocalMotionEnabled = staticCompositionLocalOf { true }

private val CampusColorScheme = lightColorScheme(
    primary = AccentIndigo,
    secondary = AccentViolet,
    background = BgCanvas,
    surface = SurfaceBento,
    surfaceVariant = SurfaceElevated,
    onPrimary = TextOnAccent,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = StatusDanger,
    secondaryContainer = WarmSelection,
    onSecondaryContainer = AccentIndigo,
    outline = Color(0xFF846B4E)
)

@Composable
fun CampusOsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMotionEnabled provides ValueAnimator.areAnimatorsEnabled()) {
        MaterialTheme(
            colorScheme = CampusColorScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}

