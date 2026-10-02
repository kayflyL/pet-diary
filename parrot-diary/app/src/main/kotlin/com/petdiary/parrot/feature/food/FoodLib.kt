package com.petdiary.parrot.feature.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.FoodRepository
import com.petdiary.parrot.data.local.PetFoodPrefEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoodRowUi(
    val id: String,
    val name: String,
    val category: String,
    val safety: String,
    val isBuiltin: Boolean,
    val prefLevel: String?,   // LOVE/LIKE/NEUTRAL/DISLIKE 或 null 未标记
)

@HiltViewModel
class FoodLibViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
) : ViewModel() {

    val petId: String = checkNotNull(savedStateHandle["petId"])

    val rows: StateFlow<List<FoodRowUi>> = combine(
        foodRepository.observeAllFoods(),
        foodRepository.observePrefs(petId),
    ) { foods, prefs ->
        val prefByFood = prefs.associate { it.foodId to it.level }
        foods.map { f ->
            FoodRowUi(f.id, f.name, f.category, f.safety, f.isBuiltin, prefByFood[f.id])
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun mark(foodId: String, level: String) {
        viewModelScope.launch { foodRepository.markPref(petId, foodId, level) }
    }

    fun addCustomFood(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { foodRepository.addCustomFood(name) }
    }
}

private val CAT_LABELS = listOf(
    "全部" to null,
    "谷物种子" to "GRAIN_SEED",
    "滋养丸" to "PELLET",
    "蔬果" to "VEGGIE,FRUIT",
    "零食补给" to "TREAT",
    "自定义" to "CUSTOM",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibScreen(petId: String, onBack: () -> Unit, vm: FoodLibViewModel = hiltViewModel()) {
    val rows by vm.rows.collectAsState()
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("全部") }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食物库") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ 返回") }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加自定义食物")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            val q = query.trim()
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("🔍 搜索食物（含危险食物警示）…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                CAT_LABELS.forEach { (label, code) ->
                    FilterChip(selected = cat == label, onClick = { cat = label }, label = { Text(label) })
                }
            }

            val visible = rows
                .filter { r ->
                    // 禁吃食物默认隐藏，仅在搜索时出现（§6.3 教育设计）
                    if (r.safety == "DANGER" && q.isEmpty()) return@filter false
                    val catOk = when (cat) {
                        "全部" -> true
                        "自定义" -> !r.isBuiltin
                        else -> cat.split(",").contains(r.category)
                    }
                    q.isEmpty() || r.name.contains(q)
                }
                .sortedWith(
                    compareByDescending<FoodRowUi> { it.prefLevel == "LOVE" }
                        .thenByDescending { it.prefLevel == "LIKE" }
                        .thenBy { it.name },
                )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visible, key = { it.id }) { r ->
                    FoodRow(r) { level -> vm.mark(r.id, level) }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdd = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "＋ 添加自定义食物（比如自配混合粮）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        var customName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加自定义食物") },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("食物名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = customName.isNotBlank(),
                    onClick = {
                        vm.addCustomFood(customName)
                        customName = ""
                        showAdd = false
                    },
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun FoodRow(r: FoodRowUi, onMark: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                SafetyBadge(r.safety)
                if (!r.isBuiltin) {
                    Text("自定义", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            prefHint(r.prefLevel)?.let { hint ->
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PrefButtons(r.prefLevel) { level -> onMark(level) }
    }
}

private fun prefHint(level: String?) = when (level) {
    "LOVE" -> "❤️ 最爱"
    "LIKE" -> "👍 爱吃"
    "NEUTRAL" -> "😐 一般"
    "DISLIKE" -> "🚫 不吃"
    else -> null
}

@Composable
private fun SafetyBadge(safety: String) {
    val (bg, fg, label) = when (safety) {
        "DANGER" -> Triple(Color(0xFFF6DCDC), Color(0xFFA03030), "禁吃")
        "CAUTION" -> Triple(Color(0xFFF6EBD8), Color(0xFF9A6A1B), "适量")
        else -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "安全")
    }
    Box(
        modifier = Modifier
            .background(bg, MaterialTheme.shapes.small)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrefButtons(current: String?, onMark: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf("LOVE" to "❤️", "LIKE" to "👍", "NEUTRAL" to "😐", "DISLIKE" to "🚫").forEach { (code, icon) ->
            val on = current == code
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.small,
                    )
                    .clickable { onMark(code) },
                contentAlignment = Alignment.Center,
            ) {
                Text(icon)
            }
        }
    }
}
