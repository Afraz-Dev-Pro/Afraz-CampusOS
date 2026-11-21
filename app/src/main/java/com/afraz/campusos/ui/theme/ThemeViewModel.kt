package com.afraz.campusos.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository
) : ViewModel() {
    private val previewPalette = MutableStateFlow<CampusColorPalette?>(null)
    val selectedPalette: StateFlow<CampusColorPalette> = combine(repository.selectedPalette, previewPalette) { selected, preview ->
        preview ?: selected
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampusPalettes.default
    )
    val savedCustomPalettes: StateFlow<List<CampusColorPalette>> = repository.customPalettes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun selectPalette(id: String) {
        previewPalette.value = null
        viewModelScope.launch { repository.selectPalette(id) }
    }

    fun previewPalette(palette: CampusColorPalette) { previewPalette.value = palette }

    fun discardPreview() { previewPalette.value = null }

    fun saveCustomPalette(palette: CampusColorPalette) {
        viewModelScope.launch {
            repository.saveCustomPalette(palette)
            previewPalette.value = null
        }
    }
}

