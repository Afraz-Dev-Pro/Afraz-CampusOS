package com.afraz.campusos.ui.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val settings: DataStore<Preferences>
) {
    private val paletteIdKey = stringPreferencesKey("campus_theme_palette_id")

    val selectedPalette: Flow<CampusColorPalette> = settings.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences -> CampusPalettes.byId(preferences[paletteIdKey]) }

    suspend fun selectPalette(id: String) {
        val selected = CampusPalettes.all.firstOrNull { it.id == id } ?: return
        settings.edit { preferences -> preferences[paletteIdKey] = selected.id }
    }
}

