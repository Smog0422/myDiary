package com.example.mydiary.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

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
            val bg = colors.backgroundTop
            val r = (bg.red * 255).toInt()
            val g = (bg.green * 255).toInt()
            val b = (bg.blue * 255).toInt()
            window.statusBarColor = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

// ===== 圆角体系：按钮/输入 12dp、卡片 16dp、弹窗 20dp =====
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 按钮统一圆角 */
val ButtonShape = RoundedCornerShape(12.dp)

/** 卡片统一圆角 */
val CardShape = RoundedCornerShape(16.dp)

/** 弹窗统一圆角 */
val DialogShape = RoundedCornerShape(20.dp)

/**
 * 页面背景：亮色模式为"阳光洒落"垂直渐变（顶部暖 → 底部净），
 * 暗色模式保持纯色。
 */
@Composable
fun BackgroundGradient(
    colors: ThemeColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val brush = Brush.verticalGradient(listOf(colors.backgroundTop, colors.background))
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
    ) {
        content()
    }
}
