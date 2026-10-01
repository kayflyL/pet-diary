package com.petdiary.parrot.core.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeFormatterTest {

    private val today: Long = LocalDate.of(2026, 10, 2).toEpochDay()

    @Test
    fun 年龄跨年时显示岁和月() {
        val birth = LocalDate.of(2025, 1, 15).toEpochDay()
        assertEquals("1 岁 8 个月", AgeFormatter.formatAge(birth, today))
    }

    @Test
    fun 年龄不足一月时只显示天数() {
        val birth = LocalDate.of(2026, 9, 27).toEpochDay()
        assertEquals("5 天", AgeFormatter.formatAge(birth, today))
    }

    @Test
    fun 未填生日或未来日期返回null() {
        assertNull(AgeFormatter.formatAge(null, today))
        assertNull(AgeFormatter.formatAge(today + 5, today))
    }

    @Test
    fun 陪伴时间优先用到家日() {
        val gotcha = LocalDate.of(2026, 1, 15).toEpochDay()
        val birth = LocalDate.of(2025, 1, 15).toEpochDay()
        // 到家 8 个月 17 天，出壳则是 1 岁 8 个月 → 应取较短者
        assertEquals("8 个月 17 天", AgeFormatter.formatCompanion(gotcha, birth, today))
    }

    @Test
    fun 陪伴时间超过一年() {
        val gotcha = LocalDate.of(2024, 8, 15).toEpochDay()
        assertEquals("2 年 1 个月", AgeFormatter.formatCompanion(gotcha, null, today))
    }

    @Test
    fun 陪伴时间缺失时返回null() {
        assertNull(AgeFormatter.formatCompanion(null, null, today))
    }
}
