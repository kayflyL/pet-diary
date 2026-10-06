package com.petdiary.parrot.feature.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import kotlin.math.min

/**
 * Lucide 描边图标（与原型图.html 的 symbol 数据同源，24×24 / 描边 2 / 圆角端点）。
 * 语义名：activity=体重曲线、leaf=植物性食物、camera、pen、wallet。
 */
@Composable
fun LucideIcon(name: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = min(size.width, size.height) / 24f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 2f
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = tint.toArgb()
                }
                lucidePaths(name).forEach { p -> canvas.nativeCanvas.drawPath(p, paint) }
                if (name == "camera") {
                    canvas.nativeCanvas.drawCircle(12f, 13f, 3f, paint)
                }
            }
        }
    }
}

private val lucideCache = mutableMapOf<String, List<android.graphics.Path>>()

private fun lucidePaths(name: String): List<android.graphics.Path> = lucideCache.getOrPut(name) {
    LUCIDE_DATA[name]!!.map { PathParser.createPathFromPathData(it) }
}

private val LUCIDE_DATA: Map<String, List<String>> = mapOf(
    "activity" to listOf("M22 12h-4l-3 9L9 3l-3 9H2"),
    "leaf" to listOf(
        "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10Z",
        "M2 21c0-3 1.9-5.4 5.1-6C9.5 14.5 12 13 13 12",
    ),
    "camera" to listOf("M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"),
    "pen" to listOf("M12 20h9", "M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"),
    "wallet" to listOf(
        "M21 12V7H5a2 2 0 0 1 0-4h14v4",
        "M3 5v14a2 2 0 0 0 2 2h16v-5",
        "M18 12a2 2 0 0 0 0 4h4v-4Z",
    ),
)
