package com.petdiary.parrot.feature.care

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petdiary.parrot.data.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(onBack: () -> Unit, vm: ExpensesViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsState()
    val period by vm.period.collectAsState()
    val total = entries.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("花费统计") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ 返回") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            androidx.compose.material3.SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                listOf("WEEK" to "周", "MONTH" to "月", "YEAR" to "年").forEachIndexed { i, (code, label) ->
                    SegmentedButton(
                        selected = period == code,
                        onClick = { vm.period.value = code },
                        label = { Text(label) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "¥" + "%.2f".format(total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        periodLabel(period) + "合计 · " + entries.size + " 笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    val byCat = entries.groupBy { it.category }
                        .map { (c, list) -> Triple(c, list.sumOf { it.amount }, list.size) }
                        .sortedByDescending { it.second }
                    val grand = maxOf(byCat.sumOf { it.second }, 1.0)
                    byCat.forEach { (cat, amount, n) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ExpenseRepository.labelOf(cat),
                                modifier = Modifier.width(56.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((amount / grand).toFloat().coerceIn(0.05f, 1f))
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                            }
                            Text(
                                "¥" + "%.0f".format(amount),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            Text("最近开销", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(entries, key = { it.id }) { e ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ExpenseRepository.labelOf(e.category),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    (e.note ?: "") + " · " + SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date(e.spentAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "¥" + "%.2f".format(e.amount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

private fun periodLabel(p: String) = when (p) {
    "WEEK" -> "本周"
    "YEAR" -> "今年"
    else -> "本月"
}
