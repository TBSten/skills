package com.example.ui.designSystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

interface AppShapes {
    val medium: Shape
    val full: Shape
}

internal object DefaultAppShapes : AppShapes {
    override val medium: Shape = RoundedCornerShape(16.dp)
    override val full: Shape = RoundedCornerShape(100)
}

internal val AppShapes.asMaterial: Shapes
    get() = Shapes(
        medium = this.medium,
    )
