package com.example.mydiary.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * 统计聚合器：纯函数，给定打卡事件 + 时间范围 → 按标签分组的计数。
 *
 * 规则（见 spec）：
 * - 自然周/月/年边界
 * - tagId=null 的事件归入"已删除标签"分组
 * - 空数据返回空列表
 */
object StatsAggregator {

    data class TagCount(
        val tagId: Int?,      // null = 已删除标签
        val tagName: String,  // 预解析好的名称（含"已删除标签"）
        val count: Int,
    )

    enum class Granularity { WEEK, MONTH, YEAR }

    /**
     * 聚合指定粒度、以 [reference] 为锚点的时间范围内的打卡。
     * @param checkIns 全部打卡事件
     * @param tagNames tagId → 名称映射（已删除的标签不在此 map 中）
     * @param granularity 周/月/年
     * @param reference 锚点日期（"当前"周/月/年）
     * @return 按 count 降序排列的 TagCount 列表
     */
    fun aggregate(
        checkIns: List<CheckIn>,
        tagNames: Map<Int, String>,
        granularity: Granularity,
        reference: LocalDate,
    ): List<TagCount> {
        val (start, end) = rangeFor(granularity, reference)

        val inRange = checkIns.filter { it.timestamp in start..end }
        if (inRange.isEmpty()) return emptyList()

        // 按 tagId 分组计数
        val grouped = inRange.groupBy { it.tagId }

        return grouped.map { (tagId, events) ->
            TagCount(
                tagId = tagId,
                tagName = tagId?.let { tagNames[it] } ?: "已删除标签",
                count = events.size,
            )
        }.sortedByDescending { it.count }
    }

    /**
     * 计算 [reference] 所属时间范围的 [start, end) 毫秒边界。
     */
    fun rangeFor(granularity: Granularity, reference: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return when (granularity) {
            Granularity.WEEK -> {
                val monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val nextMonday = monday.plusWeeks(1)
                Pair(monday.atStartOfDay(zone).toInstant().toEpochMilli(),
                     nextMonday.atStartOfDay(zone).toInstant().toEpochMilli())
            }
            Granularity.MONTH -> {
                val ym = YearMonth.from(reference)
                Pair(ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                     ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli())
            }
            Granularity.YEAR -> {
                val year = reference.year
                Pair(LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
                     LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli())
            }
        }
    }

    /**
     * 生成用于 UI 的周期标签文本，如"第 29 周""7 月""2026 年"。
     */
    fun periodLabel(granularity: Granularity, reference: LocalDate): String = when (granularity) {
        Granularity.WEEK -> "${reference.year}年第 ${weekNumber(reference)} 周"
        Granularity.MONTH -> "${reference.monthValue} 月"
        Granularity.YEAR -> "${reference.year} 年"
    }

    private fun weekNumber(date: LocalDate): Int {
        // 从 PeriodGenerator.weekKey() 提取周数，保证与同步服务一致
        val key = PeriodGenerator.weekKey(date) // e.g. "2026-W29"
        return key.substringAfterLast("W").toInt()
    }
}
