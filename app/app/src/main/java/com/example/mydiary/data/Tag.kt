package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签（Tag）：用户自定义的记录类别，是打卡的唯一分类维度。
 * 见 GLOSSARY.md 与 ADR 0002（标签即类型）。
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    /** ARGB 整型颜色，如 0xFF6200EE.toInt() */
    val color: Int,
    val createdAt: Long,
)