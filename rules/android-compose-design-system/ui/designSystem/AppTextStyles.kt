package com.example.ui.designSystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

interface AppTextStyles {
    val body: TextStyle
}

internal object DefaultAppTextStyles : AppTextStyles {
    override val body: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 19.sp,
    )
}

internal val AppTextStyles.asMaterial: Typography
    get() = Typography(
        bodyMedium = this.body,
    )
