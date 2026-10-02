package com.petdiary.parrot.core.util

import com.petdiary.parrot.data.local.WeightLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeightTrendTest {

    private val day = 86_400_000L

    private fun w(grams: Double, at: Long) =
        WeightLogEntity(id = "w$at$grams", petId = "p", grams = grams, weighedAt = at, createdAt = at, updatedAt = at)

    @Test
    fun 连续三次下降触发提醒() {
        val t0 = 0L
        val list = listOf(w(42.0, t0), w(41.5, t0 + day), w(41.0, t0 + 2 * day))
        val alert = detectWeightTrend(list)
        assertNotNull(alert)
        assertEquals(true, alert!!.message.contains("连续 3 次"))
    }

    @Test
    fun 体重平稳不触发() {
        val list = listOf(w(41.0, 0L), w(41.5, day), w(42.0, 2 * day))
        assertNull(detectWeightTrend(list))
    }

    @Test
    fun 跨五天降幅超过百分之五触发() {
        val list = listOf(w(44.0, 0L), w(41.0, 5 * day))
        val alert = detectWeightTrend(list)
        assertNotNull(alert)
        assertEquals(true, alert!!.message.contains("5 天前下降了 6.8%"))
    }

    @Test
    fun 跨五天降幅不足不触发() {
        val list = listOf(w(44.0, 0L), w(43.5, 5 * day))
        assertNull(detectWeightTrend(list))
    }

    @Test
    fun 单条记录不触发() {
        assertNull(detectWeightTrend(listOf(w(41.0, 0L))))
    }
}
