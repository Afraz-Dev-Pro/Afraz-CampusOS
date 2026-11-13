package com.afraz.campusos.core.designsystem

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)), shape)
            .padding(18.dp),
        content = content
    )
}
