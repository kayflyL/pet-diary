package com.petdiary.parrot.core.model

/** 品种目录：id 用于库内引用，参考体重区间（克）供 M3 曲线叠加（设计文档 §3.3） */
data class Species(val id: String, val name: String, val refMinG: Int?, val refMaxG: Int?)

object SpeciesCatalog {
    val all = listOf(
        Species("cockatiel", "玄凤鹦鹉", 78, 105),
        Species("budgerigar", "虎皮鹦鹉", 33, 45),
        Species("lovebird", "牡丹鹦鹉", 42, 60),
        Species("monk_parakeet", "和尚鹦鹉", 95, 120),
        Species("green_cheek", "小太阳鹦鹉", 58, 80),
        Species("caique", "凯克鹦鹉", 145, 175),
        Species("african_grey", "灰鹦鹉", 380, 540),
        Species("other", "其他", null, null),
    )

    fun nameOf(id: String): String = all.firstOrNull { it.id == id }?.name ?: id

    fun refRangeOf(id: String): Pair<Int, Int>? =
        all.firstOrNull { it.id == id }?.let { s -> s.refMinG?.let { min -> s.refMaxG?.let { max -> min to max } } }
}

/** 性格标签预设（可扩展） */
object PersonalityTags {
    val presets = listOf("粘人", "话痨", "高冷", "胆小", "吃货", "戏精", "手养娃", "社牛")
}

/** 来源预设 */
object BirdSources {
    val presets = listOf("宠物店", "繁育者", "鸟友转让", "救助", "其他")
}

/** 性别判定方式 */
object GenderMethods {
    val presets = listOf("DNA检测", "行为观察", "虹膜特征", "摸骨", "未知")
}
