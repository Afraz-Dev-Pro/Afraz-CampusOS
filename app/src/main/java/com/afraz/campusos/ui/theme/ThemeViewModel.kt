package com.afraz.campusos.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository
) : ViewModel() {
    val selectedPalette: StateFlow<CampusColorPalette> = repository.selectedPalette.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CampusPalettes.default
    )

    fun selectPalette(id: String) {
        viewModelScope.launch { repository.selectPalette(id) }
    }
}

