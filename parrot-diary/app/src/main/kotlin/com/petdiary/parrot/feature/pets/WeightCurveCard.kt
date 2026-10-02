package com.petdiary.parrot.feature.pets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.petdiary.parrot.core.util.WeightTrendAlert
import com.petdiary.parrot.core.util.detectWeightTrend
import com.petdiary.parrot.data.local.WeightLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeightTrendAlertUi(val message: String)

fun detectTrendAlertUi(sortedAsc: List<WeightLogEntity>): WeightTrendAlertUi? =
    detectWeightTrend(sortedAsc)?.let { WeightTrendAlertUi(it.message) }

/** 体重曲线卡：品种健康区间色带 + 折线 + 异常趋势提示（APP设计文档.md §3.3 / M3.5） */
@Composable
fun WeightCurveCard(
    weights: List<WeightLogEntity>,
    refRangeG: Pair<Int, Int>?,
    modifier: Modifier = Modifier,
) {
    val sorted = weights.sortedBy { it.weighedAt }
    val alert = remember(sorted) { detectTrendAlertUi(sorted) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (alert != null) {
            TrendBanner(alert.message)
            Spacer(Modifier.height(10.dp))
        }

        if (sorted.size < 2) {
            Text(
                if (sorted.size == 1) "已有 1 次记录：${sorted[0].grams} g\n再记一次，这里就会长出曲线 📈"
                else "记录两次体重后，这里会出现健康曲线",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val pri = MaterialTheme.colorScheme.primary
        val bandColor = pri.copy(alpha = 0.08f)
        val lineColor = pri
        val dotColor = pri
        val gridColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(top = 4.dp),
        ) {
            val values = sorted.map { it.grams }
            val refMin = refRangeG?.first?.toDouble()
            val refMax = refRangeG?.second?.toDouble()
            var lo = minOf(values.min(), refMin ?: values.min())
            var hi = maxOf(values.max(), refMax ?: values.max())
            val pad = (hi - lo).coerceAtLeast(1.0) * 0.12
            lo -= pad; hi += pad

            fun yOf(grams: Double): Float =
                (size.height * (1f - ((grams - lo) / (hi - lo)).toFloat())).toFloat()

            fun xOf(index: Int): Float {
                val usable = size.width - 20.dp.toPx()
                return 10.dp.toPx() + usable * index / (sorted.size - 1)
            }

            // 品种健康区间色带
            if (refMin != null && refMax != null) {
                val top = yOf(refMax)
                val bottom = yOf(refMin)
                drawRoundRect(
                    color = bandColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, bottom - top),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                )
            }

            // 底部网格线
            drawLine(gridColor, Offset(0f, size.height - 1), Offset(size.width, size.height - 1), strokeWidth = 1f)

            // 折线
            val path = androidx.compose.ui.graphics.Path()
            sorted.forEachIndexed { i, w ->
                val p = Offset(xOf(i), yOf(w.grams))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            // 数据点（最新一点放大）
            sorted.forEachIndexed { i, w ->
                val c = Offset(xOf(i), yOf(w.grams))
                if (i == sorted.size - 1) {
                    drawCircle(Color.White, radius = 7.dp.toPx(), center = c)
                    drawCircle(dotColor, radius = 6.dp.toPx(), center = c)
                } else {
                    drawCircle(dotColor, radius = 4.dp.toPx(), center = c)
                }
            }
        }

        // 日期标签 + 图例
        val zone = ZoneId.systemDefault()
        fun dateLabel(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("M.d"))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateLabel(sorted.first().weighedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            refRangeG?.let { (lo, hi) ->
                Text("健康区间 $lo–$hi g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(dateLabel(sorted.last().weighedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🧡", modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(9.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
