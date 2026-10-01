package com.petdiary.parrot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PetEntity::class,
        WeightLogEntity::class,
        FeedLogEntity::class,
        DiaryEntryEntity::class,
        DiaryMediaEntity::class,
        HealthRecordEntity::class,
        ReminderEntity::class,
        ExpenseLogEntity::class,
        FoodEntity::class,
        PetFoodPrefEntity::class,
        PlaceEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ParrotDiaryDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun feedLogDao(): FeedLogDao
    abstract fun diaryDao(): DiaryDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun reminderDao(): ReminderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun foodDao(): FoodDao
    abstract fun petFoodPrefDao(): PetFoodPrefDao
    abstract fun placeDao(): PlaceDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val NAME = "parrot_diary.db"
    }
}
