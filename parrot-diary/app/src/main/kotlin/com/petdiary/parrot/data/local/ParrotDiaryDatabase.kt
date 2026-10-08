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
        SpeciesEntity::class,
        PersonalityPresetEntity::class,
    ],
    version = 5,
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
    abstract fun speciesDao(): SpeciesDao
    abstract fun personalityPresetDao(): PersonalityPresetDao

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
            }
        }

        /** v3→v4：首页英雄卡宽幅取景图（用户手裁，所见即所得） */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN heroPath TEXT DEFAULT NULL")
            }
        }

        /** v4→v5：品种/个性预设落库可增删 + 鸟级健康区间/自定义强调色 */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS species (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "weightMinG REAL, " +
                        "weightMaxG REAL, " +
                        "isBuiltin INTEGER NOT NULL DEFAULT 0)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_species_name ON species(name)")
                listOf(
                    "'cockatiel','玄凤鹦鹉',78,105",
                    "'budgerigar','虎皮鹦鹉',33,45",
                    "'lovebird','牡丹鹦鹉',42,60",
                    "'monk_parakeet','和尚鹦鹉',95,120",
                    "'green_cheek','小太阳鹦鹉',58,80",
                    "'caique','凯克鹦鹉',145,175",
                    "'african_grey','灰鹦鹉',380,540",
                    "'other','其他',NULL,NULL",
                ).forEach { db.execSQL("INSERT INTO species (id,name,weightMinG,weightMaxG,isBuiltin) VALUES ($it,1)") }
                db.execSQL("CREATE TABLE IF NOT EXISTS personality_presets (tag TEXT NOT NULL PRIMARY KEY)")
                listOf("粘人", "话痨", "高冷", "胆小", "吃货", "戏精", "手养娃", "社牛")
                    .forEach { db.execSQL("INSERT INTO personality_presets (tag) VALUES ('$it')") }
                db.execSQL("ALTER TABLE pets ADD COLUMN speciesName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pets ADD COLUMN refWeightMinG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE pets ADD COLUMN refWeightMaxG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE pets ADD COLUMN accentColor INTEGER DEFAULT NULL")
                db.execSQL(
                    "UPDATE pets SET speciesName = CASE speciesId " +
                        "WHEN 'cockatiel' THEN '玄凤鹦鹉' WHEN 'budgerigar' THEN '虎皮鹦鹉' " +
                        "WHEN 'lovebird' THEN '牡丹鹦鹉' WHEN 'monk_parakeet' THEN '和尚鹦鹉' " +
                        "WHEN 'green_cheek' THEN '小太阳鹦鹉' WHEN 'caique' THEN '凯克鹦鹉' " +
                        "WHEN 'african_grey' THEN '灰鹦鹉' WHEN 'other' THEN '其他' " +
                        "ELSE speciesId END",
                )
            }
        }
    }
}
