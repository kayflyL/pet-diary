package com.petdiary.parrot.data

import com.petdiary.parrot.data.local.CategoryTotalRow
import com.petdiary.parrot.data.local.ExpenseDao
import com.petdiary.parrot.data.local.ExpenseLogEntity
import com.petdiary.parrot.data.local.ReminderDao
import com.petdiary.parrot.data.local.ReminderEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** 周期照护提醒（驱虫/化毛/剪指甲…）+「记一次自动顺延」 */
@Singleton
class ReminderRepository @Inject constructor(private val dao: ReminderDao) {

    data class Preset(val kind: String, val title: String, val intervalDays: Int)

    /** 鸟用预设周期（APP设计文档.md §6 周期照护） */
    val presets = listOf(
        Preset("DEWORM_IN", "体内驱虫", 90),
        Preset("DEWORM_OUT", "体外驱虫", 30),
        Preset("HAIRBALL", "化毛", 14),
        Preset("NAIL", "剪指甲", 30),
        Preset("BATH", "洗澡", 15),
        Preset("CAGE_CLEAN", "清理笼子", 7),
    )

    fun observeAll(): Flow<List<ReminderEntity>> = dao.observeAll()
    fun observeEnabled(): Flow<List<ReminderEntity>> = dao.observeEnabled()

    /** 「今天」页到期提醒：enabled 列表（界面层按 nextAt 过滤展示） */
    fun observeDue(): Flow<List<ReminderEntity>> = dao.observeEnabled()

    suspend fun add(kind: String, title: String, intervalDays: Int, nextAt: Long, petId: String? = null) {
        dao.upsert(
            ReminderEntity(
                id = UUID.randomUUID().toString(),
                petId = petId,
                kind = kind,
                title = title,
                intervalDays = intervalDays,
                nextAt = nextAt,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addFromPreset(preset: Preset, now: Long) = add(preset.kind, preset.title, preset.intervalDays, now + preset.intervalDays * 86_400_000L)

    /** 记一次：完成本次并自动顺延下一周期 */
    suspend fun completeOnce(id: String) = dao.completeAndReschedule(id, System.currentTimeMillis())

    suspend fun delete(id: String) = dao.delete(id)
}

/** 养宠开销记账（调研：养宠记式分类+周月年统计） */
@Singleton
class ExpenseRepository @Inject constructor(private val dao: ExpenseDao) {

    companion object {
        val CATEGORIES = listOf(
            "FOOD" to "粮食",
            "TOY" to "玩具",
            "SUPPLY" to "用品",
            "MEDICAL" to "医疗",
            "GROOMING" to "洗护",
            "OTHER" to "其他",
        )

        fun labelOf(category: String) = CATEGORIES.firstOrNull { it.first == category }?.second ?: "其他"
    }

    fun observeSince(since: Long): Flow<List<ExpenseLogEntity>> = dao.observeSince(since)

    suspend fun totalSince(since: Long): Double = dao.totalSince(since)

    suspend fun categoryTotalsSince(since: Long): List<CategoryTotalRow> = dao.categoryTotalsSince(since)

    suspend fun add(petId: String?, category: String, amount: Double, note: String? = null, at: Long = System.currentTimeMillis()) {
        dao.insert(
            ExpenseLogEntity(
                id = UUID.randomUUID().toString(),
                petId = petId,
                category = category,
                amount = amount,
                spentAt = at,
                note = note,
                createdAt = at,
                updatedAt = at,
            ),
        )
    }
}
