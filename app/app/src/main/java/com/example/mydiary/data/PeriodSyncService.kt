package com.example.mydiary.data

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * 周期自动同步：按模板的触发日规则检查并生成实例。
 * 在 App 启动时调用（MainActivity.onCreate）。
 *
 * V2 规则：
 * - hidden 或 !active → 跳过
 * - 已有实例 → 跳过（幂等）
 * - backfill=false 且今天 < 本周期触发日 → 跳过
 * - 否则生成
 */
class PeriodSyncService(
    private val taskRepository: TaskRepository,
    private val templateProvider: suspend () -> List<TaskTemplate>,
) {

    /**
     * 同步所有模板的缺失周期。
     * @return 生成的实例总数
     */
    suspend fun sync(): Int {
        val templates = templateProvider()
        if (templates.isEmpty()) return 0

        val today = LocalDate.now()
        var totalGenerated = 0

        for (template in templates) {
            if (!shouldGenerate(template, today)) continue
            val periodKey = currentPeriodKey(template, today)
            totalGenerated += taskRepository.generateFromTemplate(template.id, periodKey)
        }
        return totalGenerated
    }

    /**
     * 判断是否应该为 [template] 生成当前周期的实例。
     */
    private suspend fun shouldGenerate(template: TaskTemplate, today: LocalDate): Boolean {
        // 停止或隐藏 → 不生成
        if (!template.active || template.hidden) return false

        val periodKey = currentPeriodKey(template, today)

        // 幂等：已有实例则跳过
        val existing = taskRepository.observeInstancesByPeriod(periodKey).first()
        if (existing.any { it.templateId == template.id }) return false

        // 追溯控制：backfill=false 且今天还没到触发日 → 跳过
        if (!template.backfill) {
            val triggerDate = triggerDateInCurrentPeriod(template, today)
            if (today.isBefore(triggerDate)) return false
        }

        return true
    }

    /**
     * 计算模板当前周期的 periodKey。
     */
    private fun currentPeriodKey(template: TaskTemplate, today: LocalDate): String {
        return if (template.period == "weekly") {
            PeriodGenerator.weekKey(today)
        } else {
            PeriodGenerator.monthKey(today)
        }
    }

    /**
     * 计算本周期内触发日对应的日期。
     * 周任务：本周一 + (triggerDay - 1) 天
     * 月任务：本月 triggerDay 号
     */
    private fun triggerDateInCurrentPeriod(template: TaskTemplate, today: LocalDate): LocalDate {
        return if (template.period == "weekly") {
            val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            monday.plusDays((template.triggerDay - 1).toLong())
        } else {
            val day = template.triggerDay.coerceIn(1, 28)
            today.withDayOfMonth(day)
        }
    }
}
