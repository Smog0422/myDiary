package com.example.mydiary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 打卡事件的持久化接口。
 */
@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckIn): Long

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CheckIn>>

    @Query("SELECT COUNT(*) FROM check_ins WHERE tagId = :tagId AND timestamp >= :since")
    suspend fun countByTagSince(tagId: Int, since: Long): Int

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CheckIn>>
}
