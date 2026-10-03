package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务模板（Task Template）：用户维护的周期性任务清单原型。
 * 分周模板和月模板两种。见 GLOSSARY.md 与 ADR 0003。
 */
@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int,
    /** "weekly" 或 "monthly" */
    val period: String,
    val name: String,
)
