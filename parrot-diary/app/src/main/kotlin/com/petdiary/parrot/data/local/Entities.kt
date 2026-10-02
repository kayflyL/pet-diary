package com.petdiary.parrot.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 全库约定（技术方案定案.md §5 / APP设计文档.md §8）：
 * - 主键一律 UUID 字符串（备份合并去重依赖）
 * - 全表携带 createdAt/updatedAt（毫秒时间戳）
 * - 日期存 epochDay，时刻存 epochMilli，避免时区转换歧义
 * - 枚举以 String 存储（备份 JSON 可读、向前兼容）
 */

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val speciesId: String,
    val morph: String? = null,
    val legBand: String? = null,
    val source: String? = null,
    val gender: String = "U",                 // M / F / U
    val genderMethod: String? = null,         // DNA / BEHAVIOR / IRIS / BONE / NONE
    val birthDateEpochDay: Long? = null,
    val gotchaDateEpochDay: Long? = null,
    val avatarPath: String? = null,
    val personalityTagsJson: String? = null,  // ["粘人","话痨"]
    val accentIndex: Int = 0,                 // 每鸟强调色 0..5
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "weight_logs",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("petId", "weighedAt")],
)
data class WeightLogEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val grams: Double,
    val weighedAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "feed_logs",
    foreignKeys = [
        ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("petId", "fedAt"), Index("foodId")],
)
data class FeedLogEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val foodId: String? = null,
    val type: String,                         // FORMULA / PELLET / SEED / VEGGIE / WATER / OTHER
    val brand: String? = null,
    val amountText: String? = null,
    val fedAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "diary_entries",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("petId", "happenedAt")],
)
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val text: String,
    val tags: String? = null,                 // ["换羽","洗澡","放风","精神好"]（M5）
    val happenedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "diary_media",
    foreignKeys = [
        ForeignKey(entity = DiaryEntryEntity::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("entryId"), Index("placeId")],
)
data class DiaryMediaEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val path: String,                          // 私有目录内相对路径
    val type: String,                          // IMAGE / VIDEO
    val sort: Int,
    val lat: Double? = null,                   // EXIF GPS（行迹地图，v1.3）
    val lng: Double? = null,
    val takenAt: Long? = null,
    val placeId: String? = null,               // 手动归入地点
    val createdAt: Long,
)

@Entity(
    tableName = "health_records",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("petId", "happenedAt")],
)
data class HealthRecordEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val type: String,                          // VET / MEDS / DEWORM / OTHER
    val title: String,
    val detail: String? = null,
    val happenedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("petId"), Index("nextAt")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val petId: String? = null,                 // null = 全局
    val kind: String,                          // WEIGH / CAGE_CLEAN / BATH / NAIL / DEWORM_IN / DEWORM_OUT / HAIRBALL / CUSTOM
    val title: String,
    val intervalDays: Int,
    val nextAt: Long,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("spentAt"), Index("petId")],
)
data class ExpenseLogEntity(
    @PrimaryKey val id: String,
    val petId: String? = null,
    val category: String,                      // FOOD / TOY / SUPPLY / MEDICAL / GROOMING / OTHER
    val amount: Double,
    val spentAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "foods", indices = [Index("name"), Index("isBuiltin")])
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,                      // GRAIN_SEED / PELLET / VEGGIE / FRUIT / TREAT / OTHER
    val safety: String,                        // SAFE / CAUTION / DANGER
    val isBuiltin: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "pet_food_prefs",
    foreignKeys = [
        ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["petId", "foodId"], unique = true)],
)
data class PetFoodPrefEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val foodId: String,
    val level: String,                         // LOVE / LIKE / NEUTRAL / DISLIKE
    val note: String? = null,
    val updatedAt: Long,
)

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val coverMediaId: String? = null,
    val firstVisitAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,               // appearance / backup_dir / backup_enabled ...
    val value: String,
)
