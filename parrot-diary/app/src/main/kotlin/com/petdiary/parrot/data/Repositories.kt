package com.petdiary.parrot.data

import android.content.Context
import com.petdiary.parrot.data.local.FoodDao
import com.petdiary.parrot.data.local.PetDao
import com.petdiary.parrot.data.local.PetEntity
import com.petdiary.parrot.data.local.WeightLogDao
import com.petdiary.parrot.data.local.WeightLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** 首启导入一次内置食物库；重复调用安全 */
@Singleton
class FoodRepository @Inject constructor(
    private val foodDao: FoodDao,
    @ApplicationContext private val context: Context,
) {
    suspend fun ensureBuiltinImported(): Int {
        if (foodDao.builtinCount() == 0) {
            val json = context.assets.open("foods.json").bufferedReader().use { it.readText() }
            foodDao.insertAll(FoodImporter.parse(json, System.currentTimeMillis()))
        }
        return foodDao.builtinCount()
    }
}

@Singleton
class PetRepository @Inject constructor(private val petDao: PetDao) {
    fun observeActive(): Flow<List<PetEntity>> = petDao.observeActive()
    fun observeById(id: String): Flow<PetEntity?> = petDao.observeById(id)
    suspend fun getById(id: String): PetEntity? = petDao.getById(id)
    suspend fun count(): Int = petDao.count()

    suspend fun create(
        name: String,
        speciesId: String,
        accentIndex: Int = 0,
        now: Long = System.currentTimeMillis(),
    ): String {
        val id = UUID.randomUUID().toString()
        petDao.upsert(PetEntity(id = id, name = name, speciesId = speciesId, accentIndex = accentIndex, createdAt = now, updatedAt = now))
        return id
    }

    suspend fun insert(pet: PetEntity) = petDao.upsert(pet)

    suspend fun update(pet: PetEntity) = petDao.upsert(pet.copy(updatedAt = System.currentTimeMillis()))
}

@Singleton
class WeightRepository @Inject constructor(private val dao: WeightLogDao) {
    /** 3 秒录入：默认值 = 上次体重（APP设计文档.md §3.2） */
    suspend fun latestFor(petId: String): WeightLogEntity? = dao.latestForPet(petId)
    fun observeFor(petId: String): Flow<List<WeightLogEntity>> = dao.observeForPet(petId)
    fun observeLatest(petId: String): Flow<WeightLogEntity?> = dao.observeLatestForPet(petId)
    fun observeCount(petId: String): Flow<Int> = dao.observeCountForPet(petId)

    suspend fun record(petId: String, grams: Double, at: Long = System.currentTimeMillis(), note: String? = null): String {
        val id = UUID.randomUUID().toString()
        dao.insert(WeightLogEntity(id = id, petId = petId, grams = grams, weighedAt = at, note = note, createdAt = at, updatedAt = at))
        return id
    }
}
