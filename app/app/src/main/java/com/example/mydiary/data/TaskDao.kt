package com.example.mydiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {
    @Insert
    suspend fun insert(template: TaskTemplate): Long

    /** 导入用：主键冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TaskTemplate)

    @Update
    suspend fun update(template: TaskTemplate)

    @Delete
    suspend fun delete(template: TaskTemplate)

    @Query("SELECT * FROM task_templates WHERE period = :period LIMIT 1")
    fun observeByPeriod(period: String): Flow<TaskTemplate?>

    @Query("SELECT * FROM task_templates")
    fun observeAll(): Flow<List<TaskTemplate>>
}

@Dao
interface TaskItemDao {
    @Insert
    suspend fun insert(item: TaskItem): Long

    @Update
    suspend fun update(item: TaskItem)

    @Delete
    suspend fun delete(item: TaskItem)

    @Query("SELECT * FROM task_items WHERE templateId = :templateId ORDER BY sortOrder ASC")
    fun observeByTemplate(templateId: Int): Flow<List<TaskItem>>
}

@Dao
interface TaskInstanceDao {
    @Insert
    suspend fun insert(instance: TaskInstance): Long

    /** 导入用：主键冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(instance: TaskInstance)

    @Update
    suspend fun update(instance: TaskInstance)

    @Delete
    suspend fun delete(instance: TaskInstance)

    @Query("SELECT * FROM task_instances WHERE periodKey = :periodKey ORDER BY sortOrder ASC")
    fun observeByPeriod(periodKey: String): Flow<List<TaskInstance>>

    @Query("SELECT COUNT(*) FROM task_instances WHERE templateId = :templateId AND periodKey = :periodKey")
    suspend fun countByTemplateAndPeriod(templateId: Int, periodKey: String): Int

    @Query("SELECT * FROM task_instances ORDER BY periodKey DESC, sortOrder ASC")
    fun observeAll(): Flow<List<TaskInstance>>
}
