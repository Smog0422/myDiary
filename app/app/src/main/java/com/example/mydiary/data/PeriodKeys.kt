package com.example.mydiary.data

import java.time.LocalDate

/**
 * 计算当前周期的标识键。
 * 周 = "2026-W29"（简化：年-第N周，按周一为起点）
 * 月 = "2026-07"
 */
fun currentPeriodKeys(now: LocalDate = LocalDate.now()): Pair<String, String> {
    val year = now.year
    // 简化周计算：从1月1日（周一）开始数第几周
    val jan1 = LocalDate.of(year, 1, 1)
    val dayOfYear = now.dayOfYear
    // ISO 周近似：(dayOfYear + jan1 的偏移) / 7
    val week = ((dayOfYear + 6) / 7) + 1
    val weekKey = "${year}-W${week.toString().padStart(2, '0')}"
    val monthKey = "${year}-${now.monthValue.toString().padStart(2, '0')}"
    return Pair(weekKey, monthKey)
}
