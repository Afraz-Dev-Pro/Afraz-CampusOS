package com.afraz.campusos.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusColors = lightColorScheme(
    primary = AccentIndigo,
    secondary = AccentViolet,
    background = BgCanvas,
    surface = SurfaceBento,
    surfaceVariant = SurfaceElevated,
    onPrimary = Color(0xFFFFF8E9),
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
    MaterialTheme(colorScheme = CampusColors, content = content)
}

