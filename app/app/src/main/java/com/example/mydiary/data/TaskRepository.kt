package com.example.mydiary.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 任务线的领域接口：模板管理 + 实例生成/勾选。
 */
interface TaskRepository {
    // 模板
    fun observeTemplates(): Flow<List<TaskTemplate>>
    fun observeItems(templateId: Int): Flow<List<TaskItem>>
    suspend fun createTemplate(period: String, name: String): Long
    suspend fun addTaskItem(templateId: Int, title: String)
    suspend fun deleteTaskItem(item: TaskItem)

    // 实例
    fun observeInstancesByPeriod(periodKey: String): Flow<List<TaskInstance>>
    /** 从模板生成实例。返回生成的项数；已存在则返回 0（幂等）。 */
    suspend fun generateFromTemplate(templateId: Int, periodKey: String): Int
    suspend fun toggleDone(instance: TaskInstance)
}

class TaskRepositoryImpl(
    private val templateDao: TaskTemplateDao,
    private val itemDao: TaskItemDao,
    private val instanceDao: TaskInstanceDao,
) : TaskRepository {

    override fun observeTemplates(): Flow<List<TaskTemplate>> = templateDao.observeAll()

    override fun observeItems(templateId: Int): Flow<List<TaskItem>> = itemDao.observeByTemplate(templateId)

    override suspend fun createTemplate(period: String, name: String): Long {
        return templateDao.insert(TaskTemplate(id = 0, period = period, name = name))
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

    override fun observeInstancesByPeriod(periodKey: String): Flow<List<TaskInstance>> =
        instanceDao.observeByPeriod(periodKey)

    override suspend fun generateFromTemplate(templateId: Int, periodKey: String): Int {
        // 幂等：该模板+周期已有实例则跳过
        val existing = instanceDao.getByTemplateAndPeriod(templateId, periodKey)
        if (existing != null) return 0

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
}
