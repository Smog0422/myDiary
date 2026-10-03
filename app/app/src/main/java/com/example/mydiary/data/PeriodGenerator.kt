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

    /** 周 periodKey："2026-W29"（ISO 周，周一为起点） */
    fun weekKey(date: LocalDate): String {
        val year = date.year
        // ISO-8601 周数计算
        val adjustedDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY))
        val thursdayYear = adjustedDate.year
        val jan1 = LocalDate.of(thursdayYear, 1, 1)
        val week = ((adjustedDate.dayOfYear - jan1.dayOfYear) / 7) + 1
        return "${thursdayYear}-W${week.toString().padStart(2, '0')}"
    }

    /** 月 periodKey："2026-07" */
    fun monthKey(date: LocalDate): String {
        return YearMonth.from(date).toString()
    }
}
