package com.petdiary.parrot.data

import com.petdiary.parrot.data.local.PlaceDao
import com.petdiary.parrot.data.local.PlaceEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PlaceRepository @Inject constructor(private val placeDao: PlaceDao) {

    /** 「家」= 默认大本营（APP设计文档.md §7）：首次使用自动创建 */
    suspend fun ensureHomePlace() {
        if (placeDao.count() == 0) {
            val now = System.currentTimeMillis()
            placeDao.upsert(PlaceEntity(id = java.util.UUID.randomUUID().toString(), name = "家", createdAt = now, updatedAt = now))
        }
    }

    fun observeAll(): Flow<List<PlaceEntity>> = placeDao.observeAll()
}
