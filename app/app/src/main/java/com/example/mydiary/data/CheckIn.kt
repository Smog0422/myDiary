package com.example.mydiary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 打卡事件（Check-in）：一次带时间戳的行为记录。
 * 由"一个标签 + 可选备注"构成。见 GLOSSARY.md 与 ADR 0002。
 */
@Entity(
    tableName = "check_ins",
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("timestamp")]
)
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 标签 id；标签被删除后设为 null（展示为"已删除标签"） */
    val tagId: Int?,
    /** 可选备注 */
    val note: String? = null,
    val timestamp: Long,
)
