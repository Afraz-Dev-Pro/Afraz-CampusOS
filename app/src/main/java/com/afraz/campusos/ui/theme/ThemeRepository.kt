package com.afraz.campusos.ui.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val settings: DataStore<Preferences>
) {
    private val paletteIdKey = stringPreferencesKey("campus_theme_palette_id")
    private val customPalettesKey = stringPreferencesKey("campus_custom_palettes")

    val customPalettes: Flow<List<CampusColorPalette>> = settings.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { decodePalettes(it[customPalettesKey].orEmpty()) }

    private val selectedPaletteId: Flow<String?> = settings.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences -> preferences[paletteIdKey] }

    val selectedPalette: Flow<CampusColorPalette> = combine(selectedPaletteId, customPalettes) { id, custom ->
        custom.firstOrNull { it.id == id } ?: CampusPalettes.byId(id)
    }

    suspend fun selectPalette(id: String) {
        if (CampusPalettes.all.none { it.id == id } && customPalettes.first().none { it.id == id }) return
        settings.edit { preferences -> preferences[paletteIdKey] = id }
    }

    suspend fun saveCustomPalette(palette: CampusColorPalette) {
        settings.edit { preferences ->
            val updated = decodePalettes(preferences[customPalettesKey].orEmpty())
                .filterNot { it.id == palette.id } + palette
            preferences[customPalettesKey] = encodePalettes(updated)
            preferences[paletteIdKey] = palette.id
        }
    }

    private fun encodePalettes(palettes: List<CampusColorPalette>): String = palettes.joinToString(";") { palette ->
        fun hex(color: Color) = color.toArgb().toUInt().toString(16).padStart(8, '0')
        listOf(
            palette.id.replace(",", " "), palette.name.replace(Regex("[,;]"), " "),
            palette.isDark.toString(), hex(palette.canvas), hex(palette.surfacePrimary), hex(palette.surfaceElevated), hex(palette.navSurface),
            hex(palette.heroCardBg), hex(palette.heroCardText), hex(palette.heroBadgeBg), hex(palette.heroBadgeText),
            hex(palette.accentPrimary), hex(palette.accentSecondary), hex(palette.textPrimary), hex(palette.textSecondary),
            hex(palette.borderSubtle), hex(palette.statusSafe), hex(palette.statusWarning), hex(palette.statusDanger)
        ).joinToString(",")
    }

    private fun decodePalettes(encoded: String): List<CampusColorPalette> = encoded.split(';').mapNotNull { row ->
        val values = row.split(',')
        runCatching {
            require(values.size == 19)
            fun color(index: Int): Color {
                val stored = values[index].toULong(16)
                val argb = if (stored <= 0xFFFF_FFFFUL) stored.toLong().toInt() else {
                    runCatching { Color(stored).toArgb() }.getOrElse { stored.toLong().toInt() }
                }
                return Color(argb)
            }
            CampusColorPalette(
                id = values[0], name = values[1], isDark = values[2].toBooleanStrict(),
                canvas = color(3), surfacePrimary = color(4), surfaceElevated = color(5), navSurface = color(6),
                heroCardBg = color(7), heroCardText = color(8), heroBadgeBg = color(9), heroBadgeText = color(10),
                accentPrimary = color(11), accentSecondary = color(12), textPrimary = color(13),
                textSecondary = color(14), borderSubtle = color(15), statusSafe = color(16),
                statusWarning = color(17), statusDanger = color(18)
            )
        }.getOrNull()
    }
}

