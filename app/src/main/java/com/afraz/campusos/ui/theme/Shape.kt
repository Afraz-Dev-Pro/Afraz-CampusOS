package com.afraz.campusos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val BentoShape: Shape = RoundedCornerShape(20.dp)
val ActionShape: Shape = RoundedCornerShape(12.dp)
val ModalShape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

val CampusShapes = Shapes(
    small = ActionShape,
    medium = BentoShape,
    extraLarge = ModalShape
)
