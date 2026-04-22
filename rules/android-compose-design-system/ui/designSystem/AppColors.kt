package com.example.ui.designSystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

interface AppColors {
    val primary: Color
}

internal object DefaultAppColors : AppColors {
    override val primary: Color = Color(0xFF45C1D0)
}

internal val AppColors.asMaterial: androidx.compose.material3.ColorScheme
    get() = lightColorScheme(
        primary = this.primary,
    )
