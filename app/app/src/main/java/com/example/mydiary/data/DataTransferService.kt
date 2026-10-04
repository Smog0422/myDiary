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
                .put("triggerDay", it.triggerDay).put("backfill", it.backfill)
                .put("active", it.active).put("hidden", it.hidden)
        }))
        root.put("taskInstances", JSONArray(instances.map {
            JSONObject().put("id", it.id).put("templateId", it.templateId)
                .put("periodKey", it.periodKey).put("title", it.title)
                .put("sortOrder", it.sortOrder).put("done", it.done)
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
        line("CREATE TABLE IF NOT EXISTS task_templates (id INT PRIMARY KEY, period VARCHAR(10), name VARCHAR(100), trigger_day INT, backfill BOOLEAN, active BOOLEAN, hidden BOOLEAN);")
        line("CREATE TABLE IF NOT EXISTS task_instances (id INT PRIMARY KEY, template_id INT, period_key VARCHAR(20), title VARCHAR(200), sort_order INT, done BOOLEAN);")
        line()

        tags.forEach { line("INSERT INTO tags VALUES (${it.id}, '${esc(it.name)}', ${it.color}, ${it.createdAt});") }
        checkIns.forEach { line("INSERT INTO check_ins VALUES (${it.id}, ${it.tagId ?: "NULL"}, ${it.note?.let { "'${esc(it)}'" } ?: "NULL"}, ${it.timestamp});") }
        templates.forEach { line("INSERT INTO task_templates VALUES (${it.id}, '${esc(it.period)}', '${esc(it.name)}', ${it.triggerDay}, ${if (it.backfill) 1 else 0}, ${if (it.active) 1 else 0}, ${if (it.hidden) 1 else 0});") }
        instances.forEach { line("INSERT INTO task_instances VALUES (${it.id}, ${it.templateId}, '${esc(it.periodKey)}', '${esc(it.title)}', ${it.sortOrder}, ${if (it.done) 1 else 0});") }

        return sb.toString()
    }

    /** 导入 SQL 脚本（解析 INSERT 语句并写入） */
    suspend fun importSql(sql: String): Int {
        val insertRe = Regex("INSERT\\s+INTO\\s+(\\w+)\\s+VALUES\\s*\\((.*)\\)\\s*;", RegexOption.IGNORE_CASE)
        val rows = mutableMapOf<String, MutableList<List<String>>>()
        for (m in insertRe.findAll(sql)) {
            val table = m.groupValues[1].lowercase()
            if (table !in setOf("tags", "check_ins", "task_templates", "task_instances")) continue
            rows.getOrPut(table) { mutableListOf() }.add(splitCsv(m.groupValues[2]))
        }

        var count = 0
        for (v in rows["tags"].orEmpty()) {
            tagRepository.importTag(v[0].toInt(), v[1], v[2].toInt(), v[3].toLong())
            count++
        }
        for (v in rows["check_ins"].orEmpty()) {
            checkInRepository.importCheckIn(
                v[0].toInt(),
                if (v[1] == "NULL") null else v[1].toInt(),
                if (v[2] == "NULL") null else v[2],
                v[3].toLong(),
            )
            count++
        }
        for (v in rows["task_templates"].orEmpty()) {
            taskRepository.importTemplate(
                v[0].toInt(), v[1], v[2],
                if (v.size > 3) v[3].toInt() else 1,
                if (v.size > 4) v[4] == "1" else false,
                if (v.size > 5) v[5] == "1" else true,
                if (v.size > 6) v[6] == "1" else false,
            )
            count++
        }
        for (v in rows["task_instances"].orEmpty()) {
            taskRepository.importInstance(
                v[0].toInt(), v[1].toInt(), v[2], v[3],
                if (v.size > 4) v[4].toInt() else 0,
                if (v.size > 5) v[5] == "1" else false,
            )
            count++
        }
        return count
    }

    /** 拆分 SQL VALUES 中的字段：处理 '' 转义的单引号字符串与 NULL */
    private fun splitCsv(s: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inStr = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                inStr && c == '\'' && i + 1 < s.length && s[i + 1] == '\'' -> { cur.append('\''); i += 2 }
                c == '\'' -> { inStr = !inStr; i++ }
                c == ',' && !inStr -> { out.add(cur.toString().trim()); cur.setLength(0); i++ }
                else -> { cur.append(c); i++ }
            }
        }
        out.add(cur.toString().trim())
        return out
    }

    private fun esc(s: String): String = s.replace("'", "''")

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
        val tplArr = root.getJSONArray("taskTemplates")
        for (i in 0 until tplArr.length()) {
            val obj = tplArr.getJSONObject(i)
            taskRepository.importTemplate(
                obj.getInt("id"),
                obj.getString("period"),
                obj.getString("name"),
                obj.optInt("triggerDay", 1),
                obj.optBoolean("backfill", false),
                obj.optBoolean("active", true),
                obj.optBoolean("hidden", false),
            )
            count++
        }
        val instArr = root.getJSONArray("taskInstances")
        for (i in 0 until instArr.length()) {
            val obj = instArr.getJSONObject(i)
            taskRepository.importInstance(
                obj.getInt("id"),
                obj.getInt("templateId"),
                obj.getString("periodKey"),
                obj.getString("title"),
                obj.optInt("sortOrder", 0),
                obj.optBoolean("done", false),
            )
            count++
        }
        return count
    }
}

class ImportValidationException(message: String) : Exception(message)
