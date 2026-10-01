package com.petdiary.parrot.feature.pets

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petdiary.parrot.core.util.AgeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDetailScreen(
    onEdit: () -> Unit,
    onBack: () -> Unit,
    vm: BirdDetailViewModel = hiltViewModel(),
) {
    val pet by vm.bird.collectAsState()
    val latest by vm.latestWeight.collectAsState()
    val count by vm.recordCount.collectAsState()
    val ui = detailUi(pet, latest, count, LocalDate.now().toEpochDay())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鸟档案") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹ 返回", style = MaterialTheme.typography.bodyMedium) }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑档案")
                    }
                },
            )
        },
    ) { padding ->
        if (ui == null) {
            Box(Modifier.fillMaxSize().padding(padding)) // 加载中
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(62.dp).background(ui.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(ui.initial, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(ui.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(ui.speciesName, ui.genderText.takeIf { it != "未知" }, ui.ageText).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatBox(ui.latestWeightText ?: "—", "最新体重", Modifier.weight(1f))
                StatBox(ui.companionText ?: "—", "陪伴时间", Modifier.weight(1f))
                StatBox("${ui.recordCount} 条", "记一笔数量", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            if (ui.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ui.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        ) {
                            Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            InfoCard("基本信息") {
                InfoLine("品种", ui.speciesName)
                InfoLine("性别", ui.genderText)
                ui.genderMethodText?.let { InfoLine("性别判定", it) }
                ui.birthText?.let { InfoLine("出壳日 / 生日", it) }
                ui.gotchaText?.let { InfoLine("到家日", it) }
                ui.legBand?.let { InfoLine("脚环号", it) }
                ui.source?.let { InfoLine("来源", it) }
            }

            InfoCard("体重与曲线") {
                Text(
                    if (ui.latestWeightText == null) "记录第一次体重后，这里会出现健康曲线（M3 即将到来）"
                    else "体重曲线视图 M3 即将到来 · 当前最新 ${ui.latestWeightText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ui.notes?.let { InfoCard("备注") { Text(it, style = MaterialTheme.typography.bodyMedium) } }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(1.dp))
}
