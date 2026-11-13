package com.afraz.campusos.feature_toolkit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Dedicated finance destination sharing the existing local expense and split data. */
@Composable
fun FinanceScreen(modifier: Modifier = Modifier) {
    ToolkitScreen(modifier = modifier, financeOnly = true)
}
