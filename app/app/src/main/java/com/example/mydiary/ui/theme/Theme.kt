package com.example.mydiary.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun MyDiaryTheme(
    theme: AppTheme,
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = getThemeColors(theme, darkTheme)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            secondary = colors.accent,
            tertiary = colors.textSecondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.onPrimary,
            onSecondary = colors.onPrimary,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            secondary = colors.accent,
            tertiary = colors.textSecondary,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.onPrimary,
            onSecondary = colors.onPrimary,
            onBackground = colors.text,
            onSurface = colors.text,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = colors.background
            val r = (bg.red * 255).toInt()
            val g = (bg.green * 255).toInt()
            val b = (bg.blue * 255).toInt()
            window.statusBarColor = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
