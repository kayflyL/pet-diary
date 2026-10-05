package com.petdiary.parrot.data

import android.content.Context
import com.petdiary.parrot.data.local.AppSettingDao
import com.petdiary.parrot.data.local.AppSettingEntity
import com.petdiary.parrot.data.local.FeedLogDao
import com.petdiary.parrot.data.local.FoodDao
import com.petdiary.parrot.data.local.FoodEntity
import com.petdiary.parrot.data.local.FeedLogEntity
import com.petdiary.parrot.data.local.PetDao
import com.petdiary.parrot.data.local.PetEntity
import com.petdiary.parrot.data.local.PetFoodPrefDao
import com.petdiary.parrot.data.local.PetFoodPrefEntity
import com.petdiary.parrot.data.local.WeightLogDao
import com.petdiary.parrot.data.local.WeightLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 食物库 + 饮食偏好 + 喂食记录的统一门面（APP设计文档.md §6） */
@Singleton
class FoodRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val prefDao: PetFoodPrefDao,
    private val feedLogDao: FeedLogDao,
    @ApplicationContext private val context: Context,
) {

    /** 首启导入一次内置食物库（36 种）；重复调用安全 */
    suspend fun ensureBuiltinImported(): Int {
        if (foodDao.builtinCount() == 0) {
            val json = context.assets.open("foods.json").bufferedReader().use { it.readText() }
            foodDao.insertAll(FoodImporter.parse(json, System.currentTimeMillis()))
        }
        return foodDao.builtinCount()
    }

    fun observeAllFoods(): Flow<List<FoodEntity>> = foodDao.observeAll()
    fun observeFeedsFor(petId: String): Flow<List<FeedLogEntity>> = feedLogDao.observeForPet(petId)
    suspend fun foodById(id: String): FoodEntity? = foodDao.getById(id)
    suspend fun foodNames(): Map<String, String> =
        foodDao.observeAll().first().associate { it.id to it.name }

    fun observePrefs(petId: String): Flow<List<PetFoodPrefEntity>> = prefDao.observeForPet(petId)

    /** 偏好标记：已存在则更新等级，否则插入（表上唯一索引 ABORT 兜底） */
    suspend fun markPref(petId: String, foodId: String, level: String) {
        val existing = prefDao.findFor(petId, foodId)
        if (existing != null) {
            prefDao.update(existing.copy(level = level, updatedAt = System.currentTimeMillis()))
        } else {
            prefDao.insert(
                PetFoodPrefEntity(
                    id = UUID.randomUUID().toString(),
                    petId = petId,
                    foodId = foodId,
                    level = level,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun addCustomFood(name: String, category: String = "OTHER", safety: String = "SAFE") {
        foodDao.insertAll(
            listOf(
                FoodEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    category = category,
                    safety = safety,
                    isBuiltin = false,
                    createdAt = System.currentTimeMillis(),
                ),
            ),
        )
    }

    /** 编辑自定义食物；内置食物是审校过的安全知识库，拒绝改动 */
    suspend fun updateCustomFood(food: FoodEntity): Boolean {
        val old = foodDao.getById(food.id) ?: return false
        if (old.isBuiltin) return false
        foodDao.update(food.copy(name = food.name.trim(), isBuiltin = false))
        return true
    }

    suspend fun deleteCustomFood(id: String) {
        foodDao.deleteCustomById(id)
    }

    /** 记喂食：foodId 可空（自由记录） */
    suspend fun recordFeed(
        petId: String,
        foodId: String? = null,
        note: String? = null,
        at: Long = System.currentTimeMillis(),
    ) {
        val type = foodId?.let { id -> foodDao.getById(id)?.category?.let { mapType(it) } } ?: "OTHER"
        feedLogDao.insert(
            FeedLogEntity(
                id = UUID.randomUUID().toString(),
                petId = petId,
                foodId = foodId,
                type = type,
                note = note,
                fedAt = at,
                createdAt = at,
                updatedAt = at,
            ),
        )
    }

    private fun mapType(foodCategory: String) = when (foodCategory) {
        "GRAIN_SEED" -> "SEED"
        "PELLET" -> "PELLET"
        "VEGGIE", "FRUIT" -> "VEGGIE"
        else -> "OTHER"
    }
}

@Singleton
class PetRepository @Inject constructor(private val petDao: PetDao) {
    suspend fun setAvatar(petId: String, path: String, focalX: Float? = null, focalY: Float? = null) {
        petDao.getById(petId)?.let {
            petDao.upsert(it.copy(avatarPath = path, avatarFocalX = focalX, avatarFocalY = focalY))
        }
    }

    suspend fun setHeroPath(petId: String, path: String) {
        petDao.getById(petId)?.let { petDao.upsert(it.copy(heroPath = path)) }
    }
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
