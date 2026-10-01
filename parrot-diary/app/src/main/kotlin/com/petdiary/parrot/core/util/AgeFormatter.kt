package com.petdiary.parrot.core.util

import java.time.LocalDate
import java.time.Period

/** 年龄与陪伴时间格式化（纯函数，JVM 可测；设计文档 §5"自动年龄"） */
object AgeFormatter {

    /** 出壳日 → "1 岁 8 个月" / "8 个月 17 天" / "5 天"；未填或未来日期 → null */
    fun formatAge(birthEpochDay: Long?, todayEpochDay: Long): String? {
        val birth = birthEpochDay ?: return null
        if (birth > todayEpochDay) return null
        val p = Period.between(LocalDate.ofEpochDay(birth), LocalDate.ofEpochDay(todayEpochDay))
        return when {
            p.years > 0 -> "${p.years} 岁 ${p.months} 个月"
            p.months > 0 -> "${p.months} 个月 ${p.days} 天"
            else -> "${p.days} 天"
        }
    }

    /** 陪伴时间 = 到家日起算（未填则用出壳日）；→ "1 年 2 个月" / "8 个月 17 天" / "241 天"；无法计算 → null */
    fun formatCompanion(gotchaEpochDay: Long?, birthEpochDay: Long?, todayEpochDay: Long): String? {
        val start = gotchaEpochDay ?: birthEpochDay ?: return null
        if (start > todayEpochDay) return null
        val p = Period.between(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(todayEpochDay))
        return when {
            p.years > 0 -> "${p.years} 年 ${p.months} 个月"
            p.months > 0 -> "${p.months} 个月 ${p.days} 天"
            else -> "${p.days} 天"
        }
    }

    /** 性别显示文本 */
    fun genderLabel(gender: String): String = when (gender) {
        "M" -> "公"
        "F" -> "母"
        else -> "未知"
    }
}
