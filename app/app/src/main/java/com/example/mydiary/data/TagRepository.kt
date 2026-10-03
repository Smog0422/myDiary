package com.example.mydiary.data

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
    suspend fun importTag(id: Int, name: String, color: Int, createdAt: Long)
}

class TagRepositoryImpl(private val dao: TagDao) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> = dao.observeAll()

    override suspend fun create(name: String, color: Int): Long {
        return dao.insert(
            Tag(id = 0, name = name, color = color, createdAt = System.currentTimeMillis())
        )
    }

    override suspend fun rename(id: Int, name: String) {
        withTag(id) { tag -> dao.update(tag.copy(name = name)) }
    }

    override suspend fun recolor(id: Int, color: Int) {
        withTag(id) { tag -> dao.update(tag.copy(color = color)) }
    }

    override suspend fun delete(id: Int) {
        withTag(id) { tag -> dao.delete(tag) }
    }

    override suspend fun importTag(id: Int, name: String, color: Int, createdAt: Long) {
        val existing = dao.getById(id)
        if (existing == null) {
            dao.insert(Tag(id = id, name = name, color = color, createdAt = createdAt))
        }
    }

    private suspend fun withTag(id: Int, block: suspend (Tag) -> Unit) {
        val tag = dao.getById(id) ?: return
        block(tag)
    }
}
