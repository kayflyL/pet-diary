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
    version = 4,
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

        /** v1→v2：日记增加 tags 字段（M5） */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN tags TEXT DEFAULT NULL")
            }
        }

        /** v2→v3：宠物头像焦点（英雄卡宽幅背景取景用，M5） */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN avatarFocalX REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE pets ADD COLUMN avatarFocalY REAL DEFAULT NULL")
        

        /** v3→v4：首页英雄卡宽幅取景图（用户手裁，所见即所得） */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN heroPath TEXT DEFAULT NULL")
            }
        }
    }
    

        /** v3→v4：首页英雄卡宽幅取景图（用户手裁，所见即所得） */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN heroPath TEXT DEFAULT NULL")
            }
        }
    }


        /** v3→v4：首页英雄卡宽幅取景图（用户手裁，所见即所得） */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN heroPath TEXT DEFAULT NULL")
            }
        }
    }
}
