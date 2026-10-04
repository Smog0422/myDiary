package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务模板（Task Template）：用户维护的周期性任务清单原型。
 * 分周模板和月模板两种。见 GLOSSARY.md 与 ADR 0003。
 *
 * V2 新增：triggerDay（触发日）、backfill（追溯）、active（停止）、hidden（隐藏）。
 */
@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int,
    /** "weekly" 或 "monthly" */
    val period: String,
    val name: String,
    /** 触发日：周任务 = 1-7（周一=1）；月任务 = 1-28（几号） */
    val triggerDay: Int = 1,
    /** 是否追溯补生成历史周期 */
    val backfill: Boolean = false,
    /** 是否活跃（false = 停止） */
    val active: Boolean = true,
    /** 是否隐藏（隐含停止；恢复后回到停止态） */
    val hidden: Boolean = false,
)
