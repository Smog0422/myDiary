package com.example.mydiary.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * 周期生成器：纯函数，给定当前日期 + 已有实例的 periodKey 集合，
 * 返回需要新生成的 periodKey 列表。
 *
 * 边界规则（见 spec）：
 * - 自然周：周一 0 点起
 * - 自然月：1 号 0 点起
 * - 幂等：已存在的 periodKey 不重复生成
 * - 补生成：从"最近已有实例的下一期"到"当前期"逐期补齐
 */
object PeriodGenerator {

    /**
     * 计算需要生成的周 periodKey 列表。
     */
    fun missingWeeks(today: LocalDate, existingKeys: Set<String>): List<String> {
        val currentWeekKey = weekKey(today)
        val result = mutableListOf<String>()

        var cursor = today
        for (i in 0 until 53) {
            val key = weekKey(cursor)
            if (key > currentWeekKey) break
            if (key !in existingKeys) {
                result.add(key)
            }
            // 回到该周周一，再减 7 天
            val monday = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            cursor = monday.minusWeeks(1)
        }
        return result.sorted()
    }

    /**
     * 计算需要生成的月 periodKey 列表。
     */
    fun missingMonths(today: LocalDate, existingKeys: Set<String>): List<String> {
        val currentMonthKey = monthKey(today)
        val result = mutableListOf<String>()

        var cursor = YearMonth.from(today)
        for (i in 0 until 25) {
            val key = cursor.toString()
            if (key > currentMonthKey) break
            if (key !in existingKeys) {
                result.add(key)
            }
            cursor = cursor.minusMonths(1)
        }
        return result.sorted()
    }

    /** 周 periodKey："2026-W41"（ISO 8601 周，周一为起点） */
    fun weekKey(date: LocalDate): String {
        // ISO 8601：第 1 周是包含当年第一个周四的那一周（从周一开始）。
        // 1. 找到本 ISO 周的周四（决定 ISO 年）
        val thursday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
        val isoYear = thursday.year

        // 2. 该 ISO 年的 1 月 4 日必定在 W1 内，从它往回推到周一 = W1 的周一
        val jan4 = LocalDate.of(isoYear, 1, 4)
        val firstMonday = jan4.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // 3. 本周周一距 W1 周一多少周 → 周序
        val thisMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = java.time.temporal.ChronoUnit.WEEKS.between(firstMonday, thisMonday).toInt() + 1

        return "${isoYear}-W${week.toString().padStart(2, '0')}"
    }

    /** 月 periodKey："2026-07" */
    fun monthKey(date: LocalDate): String {
        return YearMonth.from(date).toString()
    }
}
