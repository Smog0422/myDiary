package com.example.mydiary.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 任务线的领域接口：模板管理 + 实例生成/勾选。
 */
interface TaskRepository {
    // 模板
    fun observeTemplates(): Flow<List<TaskTemplate>>
    /** 可见模板（非隐藏） */
    fun observeVisibleTemplates(): Flow<List<TaskTemplate>>
    /** 已隐藏模板 */
    fun observeHiddenTemplates(): Flow<List<TaskTemplate>>
    fun observeItems(templateId: Int): Flow<List<TaskItem>>
    suspend fun createTemplate(period: String, name: String, triggerDay: Int = 1): Long
    /** UI 别名：创建模板 */
    suspend fun addTemplate(name: String, period: String, triggerDay: Int = 1) { createTemplate(period, name, triggerDay) }
    suspend fun renameTemplate(templateId: Int, name: String)
    suspend fun setPeriod(templateId: Int, period: String)
    /** 完全删除模板（含其任务项）；已生成的实例不受影响 */
    suspend fun deleteTemplate(templateId: Int)
    /** 一次性更新模板所有字段（避免多个独立 set 的读-改-写竞争） */
    suspend fun updateTemplate(
        templateId: Int,
        name: String,
        period: String,
        triggerDay: Int,
        backfill: Boolean,
        active: Boolean,
        hidden: Boolean,
    )
    // ===== 对象级持久化（JS 风格：构造完整对象 → 一次写入） =====
    /** 插入完整模板对象，返回新 id */
    suspend fun insertTemplate(template: TaskTemplate): Long
    /** 插入完整任务项对象 */
    suspend fun insertItem(item: TaskItem)
    /** 用完整对象覆盖更新模板 */
    suspend fun updateTemplateObject(template: TaskTemplate)
    /** 持久化成功后触发同步：判断是否应生成当前周期实例（幂等） */
    suspend fun syncAfterSave(templateId: Int)
    /**
     * 事务级保存：在一个数据库事务中完成"更新模板 + 删除旧项 + 插入新项"。
     * 任何一步失败 → 全部回滚，保证 dialogObj 的原子性。
     */
    suspend fun saveTemplateWithItems(
        template: TaskTemplate,
        itemsToDelete: List<TaskItem>,
        itemsToInsert: List<TaskItem>,
    )
    suspend fun addTaskItem(templateId: Int, title: String)
    /** UI 别名：添加任务项 */
    suspend fun addItem(templateId: Int, text: String) { addTaskItem(templateId, text) }
    suspend fun deleteTaskItem(item: TaskItem)
    /** UI 别名：删除任务项 */
    suspend fun deleteItem(item: TaskItem) { deleteTaskItem(item) }
    suspend fun setTriggerDay(templateId: Int, triggerDay: Int)
    /** UI 别名：更新触发日 */
    suspend fun updateTriggerDay(templateId: Int, day: Int) { setTriggerDay(templateId, day) }
    suspend fun setBackfill(templateId: Int, backfill: Boolean)
    /** UI 别名：更新追溯 */
    suspend fun updateBackfill(templateId: Int, backfill: Boolean) { setBackfill(templateId, backfill) }
    suspend fun setActive(templateId: Int, active: Boolean)
    /** UI 别名：切换停止/激活 */
    suspend fun toggleActive(templateId: Int) {
        val all = observeTemplates().first()
        val t = all.find { it.id == templateId } ?: return
        setActive(templateId, !t.active)
    }
    suspend fun setHidden(templateId: Int, hidden: Boolean)
    /** UI 别名：隐藏模板 */
    suspend fun hideTemplate(templateId: Int) { setHidden(templateId, true) }
    /** UI 别名：恢复隐藏（回到停止态） */
    suspend fun unhideTemplate(templateId: Int) { setHidden(templateId, false) }

    // 实例
    fun observeInstancesByPeriod(periodKey: String): Flow<List<TaskInstance>>
    fun observeAllInstances(): Flow<List<TaskInstance>>
    /** 从模板生成实例。返回生成的项数；已存在则返回 0（幂等）。 */
    suspend fun generateFromTemplate(templateId: Int, periodKey: String): Int
    suspend fun toggleDone(instance: TaskInstance)
    /** 永久删除一条任务实例（不可恢复） */
    suspend fun deleteInstance(instance: TaskInstance)
    suspend fun importTemplate(id: Int, period: String, name: String, triggerDay: Int = 1, backfill: Boolean = false, active: Boolean = true, hidden: Boolean = false)
    suspend fun importInstance(id: Int, templateId: Int, periodKey: String, title: String, sortOrder: Int, done: Boolean)
}

class TaskRepositoryImpl(
    private val db: MyDiaryDatabase,
    private val templateDao: TaskTemplateDao,
    private val itemDao: TaskItemDao,
    private val instanceDao: TaskInstanceDao,
) : TaskRepository {

    override fun observeTemplates(): Flow<List<TaskTemplate>> = templateDao.observeAll()

    override fun observeVisibleTemplates(): Flow<List<TaskTemplate>> =
        templateDao.observeAll().map { list -> list.filter { !it.hidden } }

    override fun observeHiddenTemplates(): Flow<List<TaskTemplate>> =
        templateDao.observeAll().map { list -> list.filter { it.hidden } }

    override fun observeItems(templateId: Int): Flow<List<TaskItem>> = itemDao.observeByTemplate(templateId)

    override suspend fun createTemplate(period: String, name: String, triggerDay: Int): Long {
        return templateDao.insert(TaskTemplate(id = 0, period = period, name = name, triggerDay = triggerDay))
    }

    override suspend fun addTaskItem(templateId: Int, title: String) {
        val currentItems = itemDao.observeByTemplate(templateId).first()
        val nextSort = (currentItems.maxOfOrNull { it.sortOrder } ?: 0) + 10
        itemDao.insert(
            TaskItem(id = 0, templateId = templateId, title = title, sortOrder = nextSort)
        )
    }

    override suspend fun deleteTaskItem(item: TaskItem) {
        itemDao.delete(item)
    }

    override suspend fun renameTemplate(templateId: Int, name: String) {
        withTemplate(templateId) { templateDao.update(it.copy(name = name)) }
    }

    override suspend fun setPeriod(templateId: Int, period: String) {
        withTemplate(templateId) {
            val maxDay = if (period == "weekly") 7 else 28
            templateDao.update(it.copy(period = period, triggerDay = it.triggerDay.coerceIn(1, maxDay)))
        }
    }

    override suspend fun deleteTemplate(templateId: Int) {
        val t = observeTemplates().first().find { it.id == templateId } ?: return
        // 显式删除任务项（外键 CASCADE 也会处理）；已生成的实例不受影响
        itemDao.observeByTemplate(templateId).first().forEach { itemDao.delete(it) }
        templateDao.delete(t)
    }

    override suspend fun updateTemplate(
        templateId: Int,
        name: String,
        period: String,
        triggerDay: Int,
        backfill: Boolean,
        active: Boolean,
        hidden: Boolean,
    ) {
        withTemplate(templateId) { t ->
            val maxDay = if (period == "weekly") 7 else 28
            templateDao.update(
                t.copy(
                    name = name,
                    period = period,
                    triggerDay = triggerDay.coerceIn(1, maxDay),
                    backfill = backfill,
                    active = active,
                    hidden = hidden,
                )
            )
        }
    }

    // ===== 对象级持久化实现 =====

    override suspend fun insertTemplate(template: TaskTemplate): Long {
        return templateDao.insert(template)
    }

    override suspend fun insertItem(item: TaskItem) {
        itemDao.insert(item)
    }

    override suspend fun updateTemplateObject(template: TaskTemplate) {
        templateDao.update(template)
    }

    override suspend fun syncAfterSave(templateId: Int) {
        val t = observeTemplates().first().find { it.id == templateId } ?: return
        if (!t.active || t.hidden) return

        val today = java.time.LocalDate.now()

        // 1. 当前周期：始终尝试生成（幂等）
        val currentKey = if (t.period == "weekly") {
            PeriodGenerator.weekKey(today)
        } else {
            PeriodGenerator.monthKey(today)
        }
        generateFromTemplate(templateId, currentKey)

        // 2. 追溯：补生成历史周期（从本周期前一周/月前一个月开始，最多回溯 8 个周期）
        if (t.backfill) {
            var date = today
            repeat(8) {
                // 回退一个周期
                date = if (t.period == "weekly") {
                    date.minusWeeks(1)
                } else {
                    date.minusMonths(1)
                }
                val pastKey = if (t.period == "weekly") {
                    PeriodGenerator.weekKey(date)
                } else {
                    PeriodGenerator.monthKey(date)
                }
                // 幂等：已有实例则跳过
                generateFromTemplate(templateId, pastKey)
            }
        }
    }

    override suspend fun saveTemplateWithItems(
        template: TaskTemplate,
        itemsToDelete: List<TaskItem>,
        itemsToInsert: List<TaskItem>,
    ) {
        // 在一个事务中完成：更新模板 + 删除旧项 + 插入新项
        // 任何一步失败 → 全部回滚，保证 dialogObj 的原子性
        db.withTransaction {
            templateDao.update(template)
            itemsToDelete.forEach { itemDao.delete(it) }
            itemsToInsert.forEach { itemDao.insert(it) }
        }
    }

    override suspend fun setTriggerDay(templateId: Int, triggerDay: Int) {
        withTemplate(templateId) { it.copy(triggerDay = triggerDay.coerceIn(1, if (it.period == "weekly") 7 else 28)).let { t -> templateDao.update(t) } }
    }

    override suspend fun setBackfill(templateId: Int, backfill: Boolean) {
        withTemplate(templateId) { templateDao.update(it.copy(backfill = backfill)) }
    }

    override suspend fun setActive(templateId: Int, active: Boolean) {
        withTemplate(templateId) {
            // 停止时不影响 hidden；激活时不自动取消隐藏
            templateDao.update(it.copy(active = active))
        }
    }

    override suspend fun setHidden(templateId: Int, hidden: Boolean) {
        withTemplate(templateId) {
            if (hidden) {
                // 隐藏必然停止
                templateDao.update(it.copy(hidden = true, active = false))
            } else {
                // 恢复隐藏 → 回到停止态，不自动激活
                templateDao.update(it.copy(hidden = false, active = false))
            }
        }
    }

    override fun observeInstancesByPeriod(periodKey: String): Flow<List<TaskInstance>> =
        instanceDao.observeByPeriod(periodKey)

    override fun observeAllInstances(): Flow<List<TaskInstance>> =
        instanceDao.observeAll()

    override suspend fun generateFromTemplate(templateId: Int, periodKey: String): Int {
        // 幂等：该模板+周期已有实例则跳过
        val existingCount = instanceDao.countByTemplateAndPeriod(templateId, periodKey)
        if (existingCount > 0) return 0

        val items = itemDao.observeByTemplate(templateId).first()
        items.forEach { item ->
            instanceDao.insert(
                TaskInstance(
                    id = 0,
                    templateId = templateId,
                    periodKey = periodKey,
                    title = item.title,
                    sortOrder = item.sortOrder,
                    done = false,
                )
            )
        }
        return items.size
    }

    override suspend fun toggleDone(instance: TaskInstance) {
        instanceDao.update(instance.copy(done = !instance.done))
    }

    override suspend fun deleteInstance(instance: TaskInstance) {
        instanceDao.delete(instance)
    }

    override suspend fun importTemplate(id: Int, period: String, name: String, triggerDay: Int, backfill: Boolean, active: Boolean, hidden: Boolean) {
        templateDao.insert(TaskTemplate(id = id, period = period, name = name, triggerDay = triggerDay, backfill = backfill, active = active, hidden = hidden))
    }

    override suspend fun importInstance(id: Int, templateId: Int, periodKey: String, title: String, sortOrder: Int, done: Boolean) {
        instanceDao.insert(TaskInstance(id = id, templateId = templateId, periodKey = periodKey, title = title, sortOrder = sortOrder, done = done))
    }

    private suspend fun withTemplate(id: Int, block: suspend (TaskTemplate) -> Unit) {
        val all = templateDao.observeAll().first()
        val template = all.find { it.id == id } ?: return
        block(template)
    }
}
