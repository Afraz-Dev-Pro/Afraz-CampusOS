package com.afraz.campusos.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun NumberText(value: String, modifier: Modifier = Modifier, fontSize: TextUnit = 16.sp) {
    Text(value, modifier = modifier, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = fontSize)
}
