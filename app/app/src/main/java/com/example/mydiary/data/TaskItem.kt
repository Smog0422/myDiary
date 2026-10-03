package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 任务项（Task Item）：模板中的单个待办事项。
 */
@Entity(
    tableName = "task_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("templateId")],
)data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val templateId: Int,
    val title: String,
    val sortOrder: Int,
)
