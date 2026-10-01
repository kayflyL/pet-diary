package com.petdiary.parrot.di

import android.content.Context
import androidx.room.Room
import com.petdiary.parrot.data.local.AppSettingDao
import com.petdiary.parrot.data.local.DiaryDao
import com.petdiary.parrot.data.local.ExpenseDao
import com.petdiary.parrot.data.local.FeedLogDao
import com.petdiary.parrot.data.local.FoodDao
import com.petdiary.parrot.data.local.HealthRecordDao
import com.petdiary.parrot.data.local.ParrotDiaryDatabase
import com.petdiary.parrot.data.local.PetDao
import com.petdiary.parrot.data.local.PetFoodPrefDao
import com.petdiary.parrot.data.local.PlaceDao
import com.petdiary.parrot.data.local.ReminderDao
import com.petdiary.parrot.data.local.WeightLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ParrotDiaryDatabase =
        Room.databaseBuilder(context, ParrotDiaryDatabase::class.java, ParrotDiaryDatabase.NAME).build()

    @Provides fun petDao(db: ParrotDiaryDatabase): PetDao = db.petDao()
    @Provides fun weightLogDao(db: ParrotDiaryDatabase): WeightLogDao = db.weightLogDao()
    // [M1 调试] @Provides fun feedLogDao(db: ParrotDiaryDatabase): FeedLogDao = db.feedLogDao()
    @Provides fun diaryDao(db: ParrotDiaryDatabase): DiaryDao = db.diaryDao()
    @Provides fun healthRecordDao(db: ParrotDiaryDatabase): HealthRecordDao = db.healthRecordDao()
    @Provides fun reminderDao(db: ParrotDiaryDatabase): ReminderDao = db.reminderDao()
    @Provides fun expenseDao(db: ParrotDiaryDatabase): ExpenseDao = db.expenseDao()
    @Provides fun foodDao(db: ParrotDiaryDatabase): FoodDao = db.foodDao()
    @Provides fun petFoodPrefDao(db: ParrotDiaryDatabase): PetFoodPrefDao = db.petFoodPrefDao()
    @Provides fun placeDao(db: ParrotDiaryDatabase): PlaceDao = db.placeDao()
    @Provides fun appSettingDao(db: ParrotDiaryDatabase): AppSettingDao = db.appSettingDao()
}
