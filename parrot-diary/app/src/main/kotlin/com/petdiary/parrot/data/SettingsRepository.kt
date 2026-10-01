package com.petdiary.parrot.data

import com.petdiary.parrot.data.local.AppSettingDao
import com.petdiary.parrot.data.local.AppSettingEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 应用设置（外观三档：跟随系统/浅色/深色），存本地 AppSetting 表 */
@Singleton
class SettingsRepository @Inject constructor(private val dao: AppSettingDao) {
    val appearance: Flow<String> = dao.observe("appearance").map { it ?: "SYSTEM" }

    suspend fun setAppearance(value: String) {
        dao.set(AppSettingEntity(key = "appearance", value = value))
    }
}
