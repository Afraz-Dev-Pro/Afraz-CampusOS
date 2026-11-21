package com.afraz.campusos.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class StudioTab(val label: String) { PREMADE("Pre-made Themes"), CUSTOM("Custom Hex") }

private data class ColorRole(val label: String, val get: (CampusColorPalette) -> Color, val set: CampusColorPalette.(Color) -> CampusColorPalette)

private val editableRoles = listOf(
    ColorRole("Canvas Background", { it.canvas }, { copy(canvas = it, isDark = it.luminance() < .35f) }),
    ColorRole("Card Surface", { it.surfacePrimary }, { copy(surfacePrimary = it) }),
    ColorRole("Hero Card", { it.heroCardBg }, { copy(heroCardBg = it) }),
    ColorRole("Primary Accent", { it.accentPrimary }, { copy(accentPrimary = it) }),
    ColorRole("Primary Text", { it.textPrimary }, { copy(textPrimary = it) }),
    ColorRole("Secondary Text", { it.textSecondary }, { copy(textSecondary = it) }),
    ColorRole("Border", { it.borderSubtle }, { copy(borderSubtle = it) })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioSheet(
    selected: CampusColorPalette,
    savedCustom: List<CampusColorPalette>,
    onSelect: (CampusColorPalette) -> Unit,
    onPreview: (CampusColorPalette) -> Unit,
    onSave: (CampusColorPalette) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(StudioTab.PREMADE) }
    var working by remember(selected.id) { mutableStateOf(selected) }
    var themeName by remember(selected.id) { mutableStateOf("My Campus Theme") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = selected.surfaceElevated,
        contentColor = selected.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text("Theme Studio", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Personalize the colors across CampusOS", color = selected.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudioTab.entries.forEach { option ->
                    FilterChip(selected = tab == option, onClick = { tab = option }, label = { Text(option.label) })
                }
            }
            when (tab) {
                StudioTab.PREMADE -> {
                    val palettes = CampusPalettes.all + savedCustom
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(310.dp)) {
                        items(palettes, key = { it.id }) { palette ->
                            PaletteCard(palette, selected.id == palette.id) { onSelect(palette) }
                        }
                    }
                }
                StudioTab.CUSTOM -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.height(350.dp)) {
                        items(editableRoles, key = { it.label }) { role ->
                            key(role.label) {
                                val initial = role.get(working)
                                ColorRoleEditor(role.label, initial, selected) { color ->
                                    working = role.set(working, color)
                                    onPreview(working)
                                }
                            }
                        }
                        item {
                            FilterChip(
                                selected = working.isDark,
                                onClick = {
                                    working = working.copy(isDark = !working.isDark)
                                    onPreview(working)
                                },
                                label = { Text(if (working.isDark) "Use dark status bar icons" else "Use light status bar icons") }
                            )
                            Text("Canvas luminance sets the default icon contrast. Tap to override.", color = selected.textSecondary, fontSize = 11.sp)
                        }
                        item {
                            OutlinedTextField(
                                value = themeName,
                                onValueChange = { themeName = it.take(32) },
                                label = { Text("Custom theme name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = {
                                    working = CampusPalettes.default
                                    onReset()
                                    onPreview(working)
                                }, modifier = Modifier.weight(1f)) { Text("Reset to Default") }
                                Button(onClick = {
                                    val safeName = themeName.trim().ifBlank { "My Campus Theme" }
                                    onSave(working.copy(id = "custom-${System.currentTimeMillis()}", name = safeName))
                                }, enabled = editableRoles.all { it.get(working).toHexOrNull() != null }, modifier = Modifier.weight(1f)) {
                                    Text("Save as Custom Theme")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PaletteCard(palette: CampusColorPalette, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = palette.surfacePrimary,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accentPrimary else palette.borderSubtle)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(palette.name, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(if (palette.isDark) "Dark appearance" else "Light appearance", color = palette.textSecondary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(palette.canvas, palette.heroCardBg, palette.accentPrimary).forEach { color ->
                    Surface(Modifier.size(18.dp), shape = CircleShape, color = color, border = BorderStroke(1.dp, palette.borderSubtle)) {}
                }
            }
            if (selected) Icon(Icons.Default.Check, contentDescription = "Selected theme", tint = palette.accentPrimary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ColorRoleEditor(label: String, color: Color, palette: CampusColorPalette, onColorChange: (Color) -> Unit) {
    var input by remember(label, color.toArgb()) { mutableStateOf(color.toHex()) }
    var hue by remember(label, color.toArgb()) { mutableFloatStateOf(color.toHsv()[0]) }
    val parsed = input.toCampusColor()
    val valid = parsed != null
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.surfacePrimary).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(28.dp), color = parsed ?: color, shape = CircleShape, border = BorderStroke(1.dp, palette.borderSubtle)) {}
            Text(label, modifier = Modifier.weight(1f).padding(start = 10.dp), color = palette.textPrimary, fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = input,
                onValueChange = { raw ->
                    val normalized = if (raw.isNotEmpty() && !raw.startsWith('#')) "#$raw" else raw
                    input = normalized
                    normalized.toCampusColor()?.let(onColorChange)
                },
                singleLine = true,
                isError = !valid,
                supportingText = if (!valid) ({ Text("Enter #RRGGBB or #AARRGGBB") }) else null,
                modifier = Modifier.weight(1.3f),
                textStyle = MaterialTheme.typography.labelMedium
            )
        }
        Slider(
            value = hue,
            onValueChange = { value ->
                hue = value
                val hsv = color.toHsv()
                hsv[0] = value
                val changed = Color(android.graphics.Color.HSVToColor((hsv[3] * 255).toInt(), hsv.copyOfRange(0, 3)))
                input = changed.toHex()
                onColorChange(changed)
            },
            valueRange = 0f..360f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(palette.canvas, palette.surfacePrimary, palette.heroCardBg, palette.accentPrimary, palette.textPrimary, Color.White, Color.Black).forEach { preset ->
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(preset)
                        .clickable { input = preset.toHex(); onColorChange(preset) }
                )
            }
        }
    }
}

private fun String.toCampusColor(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    return runCatching { Color(android.graphics.Color.parseColor("#${if (hex.length == 6) "FF$hex" else hex}")) }.getOrNull()
}

private fun Color.toHex(): String = "#%08X".format(toArgb())
private fun Color.toHexOrNull(): String? = runCatching { toHex() }.getOrNull()
private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return floatArrayOf(hsv[0], hsv[1], hsv[2], alpha)
}

