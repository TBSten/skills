package com.example.ui.designSystem

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
    val shapes: AppShapes
        @Composable @ReadOnlyComposable
        get() = LocalAppShapes.current
    val textStyles: AppTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalAppTextStyles.current
}

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalAppColors = staticCompositionLocalOf<AppColors> { DefaultAppColors }

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalAppShapes = staticCompositionLocalOf<AppShapes> { DefaultAppShapes }

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalAppTextStyles = staticCompositionLocalOf<AppTextStyles> { DefaultAppTextStyles }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides DefaultAppColors,
        LocalAppShapes provides DefaultAppShapes,
        LocalAppTextStyles provides DefaultAppTextStyles,
    ) {
        MaterialTheme(
            colorScheme = AppTheme.colors.asMaterial,
            shapes = AppTheme.shapes.asMaterial,
            typography = AppTheme.textStyles.asMaterial,
        ) {
            content()
        }
    }
}
