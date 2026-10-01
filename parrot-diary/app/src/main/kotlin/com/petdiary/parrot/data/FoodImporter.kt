package com.petdiary.parrot.data

import com.petdiary.parrot.data.local.FoodEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** 内置食物库导入器：解析 assets/foods.json（36 种常见食物，APP设计文档.md 附录） */
object FoodImporter {

    @Serializable
    data class BuiltinFoodDto(
        val id: String,
        val name: String,
        val category: String,
        val safety: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** 纯函数（不触 Android API），JVM 单测直接覆盖 */
    fun parse(jsonText: String, now: Long): List<FoodEntity> =
        json.decodeFromString<List<BuiltinFoodDto>>(jsonText)
            .map { FoodEntity(id = it.id, name = it.name, category = it.category, safety = it.safety, isBuiltin = true, createdAt = now) }
}
