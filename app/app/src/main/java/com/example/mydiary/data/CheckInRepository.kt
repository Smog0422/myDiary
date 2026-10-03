package com.example.mydiary.data

import kotlinx.coroutines.flow.Flow

/**
 * 打卡事件的领域接口。
 */
interface CheckInRepository {
    fun observeRecent(limit: Int): Flow<List<CheckIn>>
    fun observeAll(): Flow<List<CheckIn>>
    suspend fun record(tagId: Int, note: String? = null): Long
    suspend fun countByTagSince(tagId: Int, since: Long): Int
}

class CheckInRepositoryImpl(private val dao: CheckInDao) : CheckInRepository {

    override fun observeRecent(limit: Int): Flow<List<CheckIn>> = dao.observeRecent(limit)

    override fun observeAll(): Flow<List<CheckIn>> = dao.observeAll()

    override suspend fun record(tagId: Int, note: String?): Long {
        return dao.insert(
            CheckIn(
                id = 0,
                tagId = tagId,
                note = note?.takeIf { it.isNotBlank() },
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun countByTagSince(tagId: Int, since: Long): Int {
        return dao.countByTagSince(tagId, since)
    }
}
