package com.petdiary.parrot.core.util

import com.petdiary.parrot.data.local.WeightLogEntity

/**
 * 体重趋势检测（APP设计文档.md §3.3）：
 * 只做"温和提醒"，不做任何诊断结论；措辞谨慎。
 * 输入必须按 weighedAt 升序排列。
 */
data class WeightTrendAlert(val message: String)

fun detectWeightTrend(
    sortedAsc: List<WeightLogEntity>,
): WeightTrendAlert? {
    if (sortedAsc.size < 2) return null
    val last = sortedAsc.last()

    // 规则一：最近连续 3 次体重下降
    if (sortedAsc.size >= 3) {
        val t = sortedAsc.takeLast(3)
        if (t[0].grams > t[1].grams && t[1].grams > t[2].grams) {
            return WeightTrendAlert("连续 3 次体重在下降，建议持续观察 🧡")
        }
    }

    // 规则二：从首次记录到现在，降幅 ≥ 5% 且跨度 ≥ 5 天
    val first = sortedAsc.first()
    val spanDays = (last.weighedAt - first.weighedAt) / 86_400_000.0
    if (spanDays >= 5 && first.grams > 0) {
        val drop = (first.grams - last.grams) / first.grams
        if (drop >= 0.05) {
            return WeightTrendAlert("体重较 ${spanDays.toInt()} 天前下降了 ${(drop * 100 * 10).toInt() / 10.0}%，建议持续观察 🧡")
        }
    }
    return null
}
