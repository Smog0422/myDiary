package com.example.mydiary.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * 周期自动同步：检查缺失周期并从模板生成实例。
 * 在 App 启动时调用（MainActivity.onCreate）。
 * WorkManager 作为后台双保险（T4 后续加入）。
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
            val periodKeys = when (template.period) {
                "weekly" -> PeriodGenerator.missingWeeks(today, existingPeriodKeys(template.id))
                "monthly" -> PeriodGenerator.missingMonths(today, existingPeriodKeys(template.id))
                else -> emptyList()
            }

            for (periodKey in periodKeys) {
                totalGenerated += taskRepository.generateFromTemplate(template.id, periodKey)
            }
        }
        return totalGenerated
    }

    private suspend fun existingPeriodKeys(templateId: Int): Set<String> {
        // 查询该模板已有的所有 periodKey
        // 简化：通过 observeAll 过滤（数据量小，个人工具可接受）
        val allInstances = taskRepository.observeAllInstances().first()
        return allInstances.filter { it.templateId == templateId }.map { it.periodKey }.toSet()
    }
}
