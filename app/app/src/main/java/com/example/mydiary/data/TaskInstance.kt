package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务实例（Task Instance）：由模板按周期生成的具体待办清单。
 * 属于某个明确的周期（如 "2026-W29" 或 "2026-07"）。
 * 见 GLOSSARY.md 与 ADR 0003。
 */
@Entity(
    tableName = "task_instances",
)
data class TaskInstance(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val templateId: Int,
    /** 周期标识：周 = "2026-W29"，月 = "2026-07" */
    val periodKey: String,
    val title: String,
    val sortOrder: Int,
    /** 是否已完成 */
    val done: Boolean = false,
)
