package com.petdiary.parrot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Upsert
    suspend fun upsert(pet: PetEntity)

    @Query("SELECT * FROM pets WHERE isArchived = 0 ORDER BY createdAt")
    fun observeActive(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets ORDER BY createdAt")
    suspend fun getAll(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getById(id: String): PetEntity?

    @Query("SELECT * FROM pets WHERE id = :id")
    fun observeById(id: String): Flow<PetEntity?>

    @Query("SELECT COUNT(*) FROM pets")
    suspend fun count(): Int

    @Query("UPDATE pets SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)
}

@Dao
interface WeightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLogEntity)

    @Query("SELECT * FROM weight_logs WHERE petId = :petId ORDER BY weighedAt")
    fun observeForPet(petId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE petId = :petId ORDER BY weighedAt DESC LIMIT 1")
    suspend fun latestForPet(petId: String): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE petId = :petId ORDER BY weighedAt DESC LIMIT 1")
    fun observeLatestForPet(petId: String): Flow<WeightLogEntity?>

    @Query("SELECT COUNT(*) FROM weight_logs WHERE petId = :petId")
    fun observeCountForPet(petId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM weight_logs")
    suspend fun count(): Int
}

@Dao
interface FeedLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FeedLogEntity)

    @Query("SELECT * FROM feed_logs WHERE petId = :petId ORDER BY fedAt DESC")
    fun observeForPet(petId: String): Flow<List<FeedLogEntity>>

    /** 最近 days 天的饮食构成（食物 → 条数），驱动鸟详情"饮食卡片" */
    @Query(
        "SELECT foodId, COUNT(*) AS cnt FROM feed_logs " +
            "WHERE petId = :petId AND foodId IS NOT NULL AND fedAt >= :since " +
            "GROUP BY foodId ORDER BY cnt DESC",
    )
    suspend fun foodCountsSince(petId: String, since: Long): List<FoodCountRow>
}

data class FoodCountRow(val foodId: String, val cnt: Int)

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<DiaryMediaEntity>)

    @Query("SELECT * FROM diary_entries WHERE petId = :petId ORDER BY happenedAt DESC")
    fun observeForPet(petId: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries ORDER BY happenedAt DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_media ORDER BY createdAt DESC")
    fun observeAllMedia(): Flow<List<DiaryMediaEntity>>

    @Query("SELECT * FROM diary_media WHERE entryId = :entryId ORDER BY sort")
    suspend fun mediaFor(entryId: String): List<DiaryMediaEntity>

    /** 照片墙：该鸟全部媒体（行迹地图按 lat/lng 过滤） */
    @Query(
        "SELECT m.* FROM diary_media m JOIN diary_entries e ON e.id = m.entryId " +
            "WHERE e.petId = :petId ORDER BY COALESCE(m.takenAt, e.happenedAt)",
    )
    fun observeMediaForPet(petId: String): Flow<List<DiaryMediaEntity>>

    /** 行迹：有 GPS 的媒体 */
    @Query("SELECT * FROM diary_media WHERE lat IS NOT NULL AND lng IS NOT NULL")
    fun observeGeotagged(): Flow<List<DiaryMediaEntity>>

    /** 行迹：带 GPS 的媒体 + 地点名（LEFT JOIN places） */
    @Query(
        "SELECT m.id AS id, m.path AS path, m.lat AS lat, m.lng AS lng, p.name AS placeName, " +
            "COALESCE(m.takenAt, e.happenedAt) AS takenAt " +
            "FROM diary_media m LEFT JOIN places p ON p.id = m.placeId " +
            "LEFT JOIN diary_entries e ON e.id = m.entryId " +
            "WHERE m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY takenAt",
    )
    fun observeGeoNamed(): Flow<List<GeoMediaRow>>

    /** 地点详情：全部带 GPS 媒体 + 地点名 + 关联日记文本/宠物，按时间升序 */
    @Query(
        "SELECT m.id AS id, m.path AS path, m.lat AS lat, m.lng AS lng, p.name AS placeName, " +
            "COALESCE(m.takenAt, e.happenedAt) AS takenAt, m.entryId AS entryId, " +
            "e.text AS entryText, e.petId AS petId, e.happenedAt AS happenedAt " +
            "FROM diary_media m LEFT JOIN places p ON p.id = m.placeId " +
            "LEFT JOIN diary_entries e ON e.id = m.entryId " +
            "WHERE m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY takenAt",
    )
    fun observePlaceMedia(): Flow<List<PlaceMediaRow>>
}

@Dao
interface HealthRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HealthRecordEntity)

    @Query("SELECT * FROM health_records WHERE petId = :petId ORDER BY happenedAt DESC")
    fun observeForPet(petId: String): Flow<List<HealthRecordEntity>>
}

@Dao
interface ReminderDao {
    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY nextAt")
    fun observeEnabled(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY nextAt")
    fun observeAll(): Flow<List<ReminderEntity>>

    /** 记一次并顺延：完成本次 → nextAt += interval */
    @Query("UPDATE reminders SET nextAt = nextAt + intervalDays * 86400000, updatedAt = :now WHERE id = :id")
    suspend fun completeAndReschedule(id: String, now: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseLogEntity)

    @Query("SELECT * FROM expenses WHERE spentAt >= :since ORDER BY spentAt DESC")
    fun observeSince(since: Long): Flow<List<ExpenseLogEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE spentAt >= :since")
    suspend fun totalSince(since: Long): Double

    @Query("SELECT category, SUM(amount) AS cnt FROM expenses WHERE spentAt >= :since GROUP BY category ORDER BY cnt DESC")
    suspend fun categoryTotalsSince(since: Long): List<CategoryTotalRow>
}

data class CategoryTotalRow(val category: String, val cnt: Double)

/** 行迹查询行：媒体 + 关联地点名 */
data class GeoMediaRow(
    val id: String,
    val path: String,
    val lat: Double,
    val lng: Double,
    val placeName: String?,
    val takenAt: Long?,
)

/** 地点详情查询行：媒体 + 地点名 + 关联日记/宠物 */
data class PlaceMediaRow(
    val id: String,
    val path: String,
    val lat: Double,
    val lng: Double,
    val placeName: String?,
    val takenAt: Long?,
    val entryId: String?,
    val entryText: String?,
    val petId: String?,
    val happenedAt: Long?,
)

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM foods WHERE isBuiltin = 1")
    suspend fun builtinCount(): Int

    @Query("SELECT * FROM foods ORDER BY isBuiltin DESC, name")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: String): FoodEntity?
}

@Dao
interface PetFoodPrefDao {
    /** 冲突即抛错（同一 pet+food 只能有一条偏好），更新走 [update]，由仓库层编排 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pref: PetFoodPrefEntity)

    @Update
    suspend fun update(pref: PetFoodPrefEntity)

    @Query("SELECT * FROM pet_food_prefs WHERE petId = :petId AND foodId = :foodId LIMIT 1")
    suspend fun findFor(petId: String, foodId: String): PetFoodPrefEntity?

    @Query("SELECT * FROM pet_food_prefs WHERE petId = :petId")
    fun observeForPet(petId: String): Flow<List<PetFoodPrefEntity>>
}

@Dao
interface PlaceDao {
    @Upsert
    suspend fun upsert(place: PlaceEntity)

    @Query("SELECT COUNT(*) FROM places")
    suspend fun count(): Int

    @Query("SELECT * FROM places ORDER BY firstVisitAt IS NULL, firstVisitAt DESC")
    fun observeAll(): Flow<List<PlaceEntity>>
}

@Dao
interface AppSettingDao {
    @Upsert
    suspend fun set(setting: AppSettingEntity)

    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM app_settings WHERE key = :key")
    fun observe(key: String): Flow<String?>
}
