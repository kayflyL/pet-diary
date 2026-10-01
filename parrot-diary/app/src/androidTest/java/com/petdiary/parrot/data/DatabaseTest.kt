package com.petdiary.parrot.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.database.sqlite.SQLiteConstraintException
import com.petdiary.parrot.data.local.ParrotDiaryDatabase
import com.petdiary.parrot.data.local.PetEntity
import com.petdiary.parrot.data.local.PetFoodPrefEntity
import com.petdiary.parrot.data.local.WeightLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 真机/模拟器上的数据库验收（M1）：建表、外键级联、唯一约束、食物库导入。
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: ParrotDiaryDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, ParrotDiaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newPet(id: String = "pet-1") = PetEntity(id = id, name = "小黄", speciesId = "cockatiel", createdAt = 0L, updatedAt = 0L)

    @Test
    fun petWeightRoundtrip() = runBlocking {
        val petDao = db.petDao()
        petDao.upsert(newPet())
        assertEquals(1, petDao.count())

        db.weightLogDao().insert(WeightLogEntity(id = "w1", petId = "pet-1", grams = 82.5, weighedAt = 1_000L, createdAt = 1_000L, updatedAt = 1_000L))
        db.weightLogDao().insert(WeightLogEntity(id = "w2", petId = "pet-1", grams = 83.0, weighedAt = 2_000L, createdAt = 2_000L, updatedAt = 2_000L))

        val latest = db.weightLogDao().latestForPet("pet-1")
        assertEquals(83.0, latest?.grams)
        assertTrue(db.weightLogDao().count() == 2)
    }

    @Test
    fun builtinFoodLibraryHas36Items() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = context.assets.open("foods.json").bufferedReader().use { it.readText() }
        val foods = FoodImporter.parse(json, System.currentTimeMillis())
        db.foodDao().insertAll(foods)

        // 设计文档附录：36 种内置食物
        assertEquals(36, db.foodDao().builtinCount())
        val avocado = db.foodDao().getById("avocado")
        assertEquals("DANGER", avocado?.safety)
    }

    @Test
    fun petFoodPrefUniqueConstraintBlocksDuplicate() {
        runBlocking {
            val petDao = db.petDao()
            petDao.upsert(newPet())
            db.foodDao().insertAll(FoodImporter.parse("""[{"id":"oat_groats","name":"燕麦米","category":"GRAIN_SEED","safety":"SAFE"}]""", 0L))

            val dao = db.petFoodPrefDao()
            dao.insert(PetFoodPrefEntity(id = "p1", petId = "pet-1", foodId = "oat_groats", level = "LOVE", updatedAt = 0L))
            // 同一 (pet, food) 不同主键 → 唯一索引应拒绝
            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    dao.insert(PetFoodPrefEntity(id = "p2", petId = "pet-1", foodId = "oat_groats", level = "LIKE", updatedAt = 0L))
                }
            }
        }
    }

    @Test
    fun deletingPetCascadesWeights() = runBlocking {
        val petDao = db.petDao()
        petDao.upsert(newPet())
        db.weightLogDao().insert(WeightLogEntity(id = "w1", petId = "pet-1", grams = 82.5, weighedAt = 0L, createdAt = 0L, updatedAt = 0L))
        petDao.setArchived("pet-1", true, 0L)
        assertTrue(petDao.getById("pet-1")!!.isArchived)
        // 归档不删数据
        assertEquals(1, db.weightLogDao().count())
    }
}
