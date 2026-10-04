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
