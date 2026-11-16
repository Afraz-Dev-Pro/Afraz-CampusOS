package com.afraz.campusos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusColorScheme = darkColorScheme(
    primary = AccentIndigo,
    secondary = AccentViolet,
    background = BgCanvas,
    surface = SurfaceBento,
    surfaceVariant = SurfaceElevated,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = StatusDanger
)

@Composable
fun CampusOsTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = CampusColorScheme,
        typography = CampusTypography,
        shapes = CampusShapes,
        content = content
    )
}
