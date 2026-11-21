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
import androidx.compose.ui.unit.dp
import android.animation.ValueAnimator
import com.afraz.campusos.ui.theme.LocalCampusColors
import com.afraz.campusos.ui.theme.LocalMotionEnabled

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val motionEnabled = LocalMotionEnabled.current && ValueAnimator.areAnimatorsEnabled()
    val colors = LocalCampusColors.current
    Column(
        modifier = modifier
            .then(if (motionEnabled) Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)) else Modifier)
            .clip(shape)
            .background(colors.surfacePrimary)
            .border(1.dp, colors.borderSubtle, shape)
            .padding(18.dp),
        content = content
    )
}

