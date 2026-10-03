package com.example.mydiary.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * 标签的领域接口：UI 只依赖这个接口，不直接依赖 DAO。
 */
interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    suspend fun create(name: String, color: Int): Long
    suspend fun rename(id: Int, name: String)
    suspend fun recolor(id: Int, color: Int)
    suspend fun delete(id: Int)
}

class TagRepositoryImpl(private val dao: TagDao, private val db: MyDiaryDatabase) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> = dao.observeAll()

    override suspend fun create(name: String, color: Int): Long {
        // 自增 id：先插入占位，Room 回填真实 id
        var newId = 0L
        db.withTransaction {
            newId = dao.insert(Tag(id = 0, name = name, color = color, createdAt = System.currentTimeMillis()))
        }
        return newId
    }

    override suspend fun rename(id: Int, name: String) {
        val tag = dao.getById(id) ?: return
        dao.update(tag.copy(name = name))
    }

    override suspend fun recolor(id: Int, color: Int) {
        val tag = dao.getById(id) ?: return
        dao.update(tag.copy(color = color))
    }

    override suspend fun delete(id: Int) {
        val tag = dao.getById(id) ?: return
        dao.delete(tag)
    }
}
