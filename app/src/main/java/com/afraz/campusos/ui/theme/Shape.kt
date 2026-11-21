package com.afraz.campusos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BentoShape = RoundedCornerShape(20.dp)
val ActionShape = RoundedCornerShape(12.dp)
val ModalShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

val CampusShapes = Shapes(
    small = ActionShape,
    medium = BentoShape,
    extraLarge = ModalShape
)
