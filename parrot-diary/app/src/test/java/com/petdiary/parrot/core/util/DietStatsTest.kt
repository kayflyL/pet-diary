package com.petdiary.parrot.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DietStatsTest {

    private val names = mapOf("oat" to "燕麦米", "sun" to "葵花子", "millet" to "黄谷子")
    private val now = 1_000_000_000L
    private val day = 86_400_000L

    @Test
    fun 构成比按次数降序且百分比正确() {
        val entries = listOf(
            (now - day) to "oat",
            (now - 2 * day) to "oat",
            (now - 3 * day) to "sun",
            (now - 40 * day) to "millet", // 超出 30 天窗口，应排除
        )
        val result = DietStats.compute(entries, names, now)
        assertEquals(2, result.size)
        assertEquals("燕麦米", result[0].name)
        assertEquals(2, result[0].count)
        assertEquals(67, result[0].pct) // 2/3 ≈ 67%
        assertEquals("葵花子", result[1].name)
        assertEquals(33, result[1].pct)
    }

    @Test
    fun 无foodId的记录被忽略() {
        val entries = listOf((now - day) to null, (now - day) to "oat")
        val result = DietStats.compute(entries, names, now)
        assertEquals(1, result.size)
        assertEquals("oat", result[0].foodId)
    }

    @Test
    fun 空记录返回空列表() {
        assertEquals(0, DietStats.compute(emptyList(), names, now).size)
    }
}
