package com.petdiary.parrot.core.util

/**
 * 饮食构成统计（APP设计文档.md §6）：最近 N 天喂食记录 → 按食物聚合的构成比。
 * 纯函数，JVM 可测。entries = (fedAt 毫秒, foodId 可空) 列表。
 */
object DietStats {

    data class FoodShare(val foodId: String, val name: String, val count: Int, val pct: Int)

    fun compute(
        entries: List<Pair<Long, String?>>,
        foodNames: Map<String, String>,
        nowMillis: Long,
        days: Int = 30,
    ): List<FoodShare> {
        val cutoff = nowMillis - days * 86_400_000L
        val counted = entries
            .filter { (at, foodId) -> at >= cutoff && foodId != null }
            .groupingBy { (_, foodId) -> foodId!! }
            .eachCount()
        val grand = counted.values.sum()
        return counted.entries
            .sortedByDescending { it.value }
            .map { (foodId, count) ->
                FoodShare(
                    foodId = foodId,
                    name = foodNames[foodId] ?: foodId,
                    count = count,
                    pct = if (grand == 0) 0 else (count * 100 + grand / 2) / grand,
                )
            }
    }
}
