package com.example.mydiary.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 三种主题配色 =====

enum class AppTheme { MORNING_MIST, WARM_SAND, PALE_SEA }

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
)

// 晨雾 — 淡绿·清新·自然
val MorningMist = ThemeColors(
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF4A6741),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF2D3B2A),
    textSecondary = Color(0xFF6B7D68),
    border = Color(0xFFE8EFE8),
    accent = Color(0xFF6B9E78),
)

// 暖砂 — 米黄·温暖·质朴
val WarmSand = ThemeColors(
    background = Color(0xFFFAF6F1),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF8B6F47),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF3D3225),
    textSecondary = Color(0xFF8B7D6B),
    border = Color(0xFFEDE5DB),
    accent = Color(0xFFC4A265),
)

// 淡海 — 雾蓝·宁静·大气
val PaleSea = ThemeColors(
    background = Color(0xFFF4F8FB),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF3D6B8E),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF1E2D3D),
    textSecondary = Color(0xFF5A7A94),
    border = Color(0xFFDCE8F0),
    accent = Color(0xFF7BA7C9),
)

fun getThemeColors(theme: AppTheme, dark: Boolean): ThemeColors {
    val base = when (theme) {
        AppTheme.MORNING_MIST -> MorningMist
        AppTheme.WARM_SAND -> WarmSand
        AppTheme.PALE_SEA -> PaleSea
    }
    if (!dark) return base
    // 暗色模式：降低亮度，保持色相
    return base.copy(
        background = Color(0xFF1A1E1A),
        surface = Color(0xFF242824),
        primary = base.accent,
        onPrimary = Color(0xFF1A1E1A),
        text = Color(0xFFE8EDE8),
        textSecondary = Color(0xFF9AA89A),
        border = Color(0xFF3A403A),
    )
}
