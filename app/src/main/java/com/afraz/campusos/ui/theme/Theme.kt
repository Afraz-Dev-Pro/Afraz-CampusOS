package com.afraz.campusos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import android.animation.ValueAnimator

val LocalMotionEnabled = staticCompositionLocalOf { true }

private val CampusColorScheme = darkColorScheme(
    primary = AccentIndigo,
    secondary = AccentViolet,
    background = BgCanvas,
    surface = SurfaceBento,
    surfaceVariant = SurfaceElevated,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = StatusDanger
)

@Composable
fun CampusOsTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    CompositionLocalProvider(LocalMotionEnabled provides ValueAnimator.areAnimatorsEnabled()) {
        MaterialTheme(
            colorScheme = CampusColorScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}

