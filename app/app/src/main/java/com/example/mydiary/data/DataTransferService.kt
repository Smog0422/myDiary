package com.example.mydiary.data

import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据导出/导入服务。
 * JSON 格式：人类可读、跨工具通用。
 */
class DataTransferService(
    private val tagRepository: TagRepository,
    private val checkInRepository: CheckInRepository,
    private val taskRepository: TaskRepository,
) {

    /**
     * 导出全量数据为 JSON 字符串。
     */
    suspend fun exportJson(): String {
        val tags = tagRepository.observeAll().first()
        val checkIns = checkInRepository.observeAll().first()
        val templates = taskRepository.observeTemplates().first()
        val allInstances = taskRepository.observeAllInstances().first()

        val root = JSONObject()

        // 标签
        val tagsArr = JSONArray()
        tags.forEach { tag ->
            tagsArr.put(
                JSONObject()
                    .put("id", tag.id)
                    .put("name", tag.name)
                    .put("color", tag.color)
                    .put("createdAt", tag.createdAt)
            )
        }
        root.put("tags", tagsArr)

        // 打卡事件
        val checkInsArr = JSONArray()
        checkIns.forEach { ci ->
            checkInsArr.put(
                JSONObject()
                    .put("id", ci.id)
                    .put("tagId", ci.tagId ?: JSONObject.NULL)
                    .put("note", ci.note ?: JSONObject.NULL)
                    .put("timestamp", ci.timestamp)
            )
        }
        root.put("checkIns", checkInsArr)

        // 任务模板
        val templatesArr = JSONArray()
        templates.forEach { t ->
            templatesArr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("period", t.period)
                    .put("name", t.name)
            )
        }
        root.put("taskTemplates", templatesArr)

        // 任务实例
        val instancesArr = JSONArray()
        allInstances.forEach { inst ->
            instancesArr.put(
                JSONObject()
                    .put("id", inst.id)
                    .put("templateId", inst.templateId)
                    .put("periodKey", inst.periodKey)
                    .put("title", inst.title)
                    .put("sortOrder", inst.sortOrder)
                    .put("done", inst.done)
            )
        }
        root.put("taskInstances", instancesArr)

        return root.toString(2) // 格式化缩进
    }

    /**
     * 导入 JSON 字符串。校验通过才写入。
     * @return 导入的记录数，或抛出 [ImportValidationException]
     */
    suspend fun importJson(json: String): Int {
        val root = parseAndValidate(json)

        var count = 0

        // 导入标签
        val tagsArr = root.getJSONArray("tags")
        for (i in 0 until tagsArr.length()) {
            val obj = tagsArr.getJSONObject(i)
            tagRepository.importTag(
                id = obj.optInt("id", 0),
                name = obj.getString("name"),
                color = obj.getInt("color"),
                createdAt = obj.getLong("createdAt"),
            )
            count++
        }

        // 导入打卡
        val checkInsArr = root.getJSONArray("checkIns")
        for (i in 0 until checkInsArr.length()) {
            val obj = checkInsArr.getJSONObject(i)
            checkInRepository.importCheckIn(
                id = obj.optInt("id", 0),
                tagId = if (obj.isNull("tagId")) null else obj.getInt("tagId"),
                note = if (obj.isNull("note")) null else obj.getString("note"),
                timestamp = obj.getLong("timestamp"),
            )
            count++
        }

        // 导入模板
        val templatesArr = root.getJSONArray("taskTemplates")
        for (i in 0 until templatesArr.length()) {
            val obj = templatesArr.getJSONObject(i)
            taskRepository.importTemplate(
                id = obj.optInt("id", 0),
                period = obj.getString("period"),
                name = obj.getString("name"),
            )
            count++
        }

        // 导入实例
        val instancesArr = root.getJSONArray("taskInstances")
        for (i in 0 until instancesArr.length()) {
            val obj = instancesArr.getJSONObject(i)
            taskRepository.importInstance(
                id = obj.optInt("id", 0),
                templateId = obj.getInt("templateId"),
                periodKey = obj.getString("periodKey"),
                title = obj.getString("title"),
                sortOrder = obj.getInt("sortOrder"),
                done = obj.getBoolean("done"),
            )
            count++
        }

        return count
    }

    private fun parseAndValidate(json: String): JSONObject {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw ImportValidationException("JSON 解析失败：${e.message}")
        }

        // 校验必需字段
        val required = listOf("tags", "checkIns", "taskTemplates", "taskInstances")
        for (field in required) {
            if (!root.has(field)) {
                throw ImportValidationException("缺少必需字段：$field")
            }
            if (root.get(field) !is JSONArray) {
                throw ImportValidationException("字段 $field 必须是数组")
            }
        }

        // 校验标签条目
        val tagsArr = root.getJSONArray("tags")
        for (i in 0 until tagsArr.length()) {
            val obj = tagsArr.getJSONObject(i)
            if (!obj.has("name")) throw ImportValidationException("标签 [$i] 缺少 name")
            if (!obj.has("color")) throw ImportValidationException("标签 [$i] 缺少 color")
        }

        return root
    }
}

class ImportValidationException(message: String) : Exception(message)
