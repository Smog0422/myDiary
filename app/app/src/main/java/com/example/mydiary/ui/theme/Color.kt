package com.example.mydiary.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 三种主题配色（清晨绿 / 午间蓝 / 傍晚黄）=====

enum class AppTheme { MORNING_MIST, WARM_SAND, PALE_SEA }

data class ThemeColors(
    val background: Color,
    /** 亮色模式渐变顶部色：模拟阳光洒落 */
    val backgroundTop: Color,
    val surface: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
)

// 清晨绿 — 晨光·清新·自然
val MorningMist = ThemeColors(
    background = Color(0xFFF7FAF7),
    backgroundTop = Color(0xFFEEF5EC),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF4A6741),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF2D3B2A),
    textSecondary = Color(0xFF6B7D68),
    border = Color(0xFFE8EFE8),
    accent = Color(0xFF6B9E78),
)

// 午间蓝 — 天光·清澈·宁静
val PaleSea = ThemeColors(
    background = Color(0xFFF4F8FB),
    backgroundTop = Color(0xFFEAF2F9),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF3D6B8E),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF1E2D3D),
    textSecondary = Color(0xFF5A7A94),
    border = Color(0xFFDCE8F0),
    accent = Color(0xFF7BA7C9),
)

// 傍晚黄 — 夕阳·温暖·质朴
val WarmSand = ThemeColors(
    background = Color(0xFFFAF6F1),
    backgroundTop = Color(0xFFFBF3E6),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF8B6F47),
    onPrimary = Color(0xFFFFFFFF),
    text = Color(0xFF3D3225),
    textSecondary = Color(0xFF8B7D6B),
    border = Color(0xFFEDE5DB),
    accent = Color(0xFFC4A265),
)

fun getThemeColors(theme: AppTheme, dark: Boolean): ThemeColors {
    val base = when (theme) {
        AppTheme.MORNING_MIST -> MorningMist
        AppTheme.WARM_SAND -> WarmSand
        AppTheme.PALE_SEA -> PaleSea
    }
    if (!dark) return base
    // 暗色模式：保持纯色（不渐变），降低亮度，保持色相
    val darkBg = when (theme) {
        AppTheme.MORNING_MIST -> Color(0xFF1A1E1A)
        AppTheme.PALE_SEA -> Color(0xFF181D22)
        AppTheme.WARM_SAND -> Color(0xFF201C17)
    }
    val darkSurface = when (theme) {
        AppTheme.MORNING_MIST -> Color(0xFF242824)
        AppTheme.PALE_SEA -> Color(0xFF232930)
        AppTheme.WARM_SAND -> Color(0xFF2B2620)
    }
    val darkBorder = when (theme) {
        AppTheme.MORNING_MIST -> Color(0xFF3A403A)
        AppTheme.PALE_SEA -> Color(0xFF333B44)
        AppTheme.WARM_SAND -> Color(0xFF423B31)
    }
    val darkTextSecondary = when (theme) {
        AppTheme.MORNING_MIST -> Color(0xFF9AA89A)
        AppTheme.PALE_SEA -> Color(0xFF93A3B1)
        AppTheme.WARM_SAND -> Color(0xFFA99C8B)
    }
    return base.copy(
        background = darkBg,
        backgroundTop = darkBg, // 暗色不渐变
        surface = darkSurface,
        primary = base.accent,
        onPrimary = darkBg,
        text = Color(0xFFE8EDE8),
        textSecondary = darkTextSecondary,
        border = darkBorder,
    )
}
