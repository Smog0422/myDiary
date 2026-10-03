package com.example.mydiary.data

import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据导出/导入服务。
 */
class DataTransferService(
    private val tagRepository: TagRepository,
    private val checkInRepository: CheckInRepository,
    private val taskRepository: TaskRepository,
) {

    /** 导出为格式化 JSON */
    suspend fun exportJson(): String {
        val tags = tagRepository.observeAll().first()
        val checkIns = checkInRepository.observeAll().first()
        val templates = taskRepository.observeTemplates().first()
        val instances = taskRepository.observeAllInstances().first()

        val root = JSONObject()
        root.put("tags", JSONArray(tags.map {
            JSONObject().put("id", it.id).put("name", it.name)
                .put("color", it.color).put("createdAt", it.createdAt)
        }))
        root.put("checkIns", JSONArray(checkIns.map {
            JSONObject().put("id", it.id).put("tagId", it.tagId ?: JSONObject.NULL)
                .put("note", it.note ?: JSONObject.NULL).put("timestamp", it.timestamp)
        }))
        root.put("taskTemplates", JSONArray(templates.map {
            JSONObject().put("id", it.id).put("period", it.period).put("name", it.name)
        }))
        root.put("taskInstances", JSONArray(instances.map {
            JSONObject().put("id", it.id).put("templateId", it.templateId)
                .put("periodKey", it.periodKey).put("title", it.title).put("done", it.done)
        }))
        return root.toString(2)
    }

    /** 导出为 SQL 脚本（建表 + INSERT） */
    suspend fun exportSql(): String {
        val tags = tagRepository.observeAll().first()
        val checkIns = checkInRepository.observeAll().first()
        val templates = taskRepository.observeTemplates().first()
        val instances = taskRepository.observeAllInstances().first()

        val sb = java.lang.StringBuilder()
        fun line(s: String = "") { sb.append(s).append("\n") }

        line("-- MyDiary SQL Export")
        line("CREATE TABLE IF NOT EXISTS tags (id INT PRIMARY KEY, name VARCHAR(100), color INT, created_at BIGINT);")
        line("CREATE TABLE IF NOT EXISTS check_ins (id BIGINT PRIMARY KEY, tag_id INT, note TEXT, timestamp BIGINT);")
        line("CREATE TABLE IF NOT EXISTS task_templates (id INT PRIMARY KEY, period VARCHAR(10), name VARCHAR(100));")
        line("CREATE TABLE IF NOT EXISTS task_instances (id INT PRIMARY KEY, template_id INT, period_key VARCHAR(20), title VARCHAR(200), done BOOLEAN);")
        line()

        tags.forEach { line("INSERT INTO tags VALUES (${it.id}, '${esc(it.name)}', ${it.color}, ${it.createdAt});") }
        checkIns.forEach { line("INSERT INTO check_ins VALUES (${it.id}, ${it.tagId ?: "NULL"}, ${it.note?.let { "'${esc(it)}'" } ?: "NULL"}, ${it.timestamp});") }
        templates.forEach { line("INSERT INTO task_templates VALUES (${it.id}, '${esc(it.period)}', '${esc(it.name)}');") }
        instances.forEach { line("INSERT INTO task_instances VALUES (${it.id}, ${it.templateId}, '${esc(it.periodKey)}', '${esc(it.title)}', ${if (it.done) 1 else 0});") }

        return sb.toString()
    }

    /** 导入 JSON，校验通过才写入 */
    suspend fun importJson(json: String): Int {
        val root = try { JSONObject(json) } catch (e: Exception) { throw ImportValidationException("JSON 解析失败：${e.message}") }
        for (field in listOf("tags", "checkIns", "taskTemplates", "taskInstances")) {
            if (!root.has(field)) throw ImportValidationException("缺少必需字段：$field")
        }

        var count = 0
        val tagsArr = root.getJSONArray("tags")
        for (i in 0 until tagsArr.length()) {
            val obj = tagsArr.getJSONObject(i)
            tagRepository.importTag(obj.getInt("id"), obj.getString("name"), obj.getInt("color"), obj.getLong("createdAt"))
            count++
        }
        val ciArr = root.getJSONArray("checkIns")
        for (i in 0 until ciArr.length()) {
            val obj = ciArr.getJSONObject(i)
            checkInRepository.importCheckIn(obj.getInt("id"), if (obj.isNull("tagId")) null else obj.getInt("tagId"), if (obj.isNull("note")) null else obj.getString("note"), obj.getLong("timestamp"))
            count++
        }
        return count
    }

    private fun esc(s: String): String = s.replace("'", "''")
}

class ImportValidationException(message: String) : Exception(message)
