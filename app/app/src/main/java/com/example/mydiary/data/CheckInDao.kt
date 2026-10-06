package com.example.mydiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 打卡事件的持久化接口。
 */
@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckIn): Long

    /** 导入用：主键冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: CheckIn)

    @Delete
    suspend fun delete(checkIn: CheckIn)

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CheckIn>>

    @Query("SELECT COUNT(*) FROM check_ins WHERE tagId = :tagId AND timestamp >= :since")
    suspend fun countByTagSince(tagId: Int, since: Long): Int

    /** 当月各标签打卡次数（首页排序用） */
    data class TagCountRow(val tagId: Int, val cnt: Int)

    @Query("SELECT tagId, COUNT(*) AS cnt FROM check_ins WHERE tagId IS NOT NULL AND timestamp >= :monthStart GROUP BY tagId")
    suspend fun countByTagSinceMonth(monthStart: Long): List<TagCountRow>

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CheckIn>>
}
