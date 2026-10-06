package com.example.mydiary.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 任务实例（Task Instance）：由模板按周期生成的具体待办清单。
 * 属于某个明确的周期（如 "2026-W29" 或 "2026-07"）。
 * 一个模板在一个周期可以有多条实例（每个子事件一条）。
 * 见 GLOSSARY.md 与 ADR 0003。
 */
@Entity(
    tableName = "task_instances",
    indices = [Index("templateId"), Index("periodKey")],
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
    @ColumnInfo(defaultValue = "0")
    val done: Boolean = false,
    /** 完成时间戳（ms）；未完成 = null。v1.1.0 新增。 */
    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,
)
