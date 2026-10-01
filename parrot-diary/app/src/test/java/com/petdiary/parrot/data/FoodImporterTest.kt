package com.petdiary.parrot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodImporterTest {

    @Test
    fun `解析内置食物 JSON 并保留安全等级`() {
        val json = """
            [
              {"id": "oat_groats", "name": "燕麦米", "category": "GRAIN_SEED", "safety": "SAFE"},
              {"id": "avocado", "name": "牛油果", "category": "FRUIT", "safety": "DANGER"},
              {"id": "sunflower_seed", "name": "葵花子", "category": "GRAIN_SEED", "safety": "CAUTION", "extra": "未知字段应忽略"}
            ]
        """.trimIndent()

        val foods = FoodImporter.parse(json, now = 42L)

        assertEquals(3, foods.size)
        assertEquals("燕麦米", foods[0].name)
        assertEquals("SAFE", foods[0].safety)
        assertEquals("DANGER", foods[1].safety)
        assertEquals("CAUTION", foods[2].safety)
        assertTrue(foods.all { it.isBuiltin })
        assertEquals(42L, foods[0].createdAt)
    }
}
