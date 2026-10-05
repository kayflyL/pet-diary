package com.petdiary.parrot.feature.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.FoodRepository
import com.petdiary.parrot.data.local.FoodEntity
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

/** 分类选项（新增/编辑表单用，code 与 FoodEntity.category 对应） */
private val CATEGORY_OPTIONS = listOf(
    "GRAIN_SEED" to "谷物种子",
    "PELLET" to "滋养丸",
    "VEGGIE" to "蔬菜",
    "FRUIT" to "水果",
    "TREAT" to "零食补给",
    "OTHER" to "其他",
)

/** 安全等级选项 */
private val SAFETY_OPTIONS = listOf(
    "SAFE" to "安全",
    "CAUTION" to "适量",
    "DANGER" to "禁吃",
)

/** 偏好标记选项（行内文字 chips） */
private val PREF_OPTIONS = listOf(
    "LOVE" to "最爱",
    "LIKE" to "爱吃",
    "NEUTRAL" to "一般",
    "DISLIKE" to "不吃",
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

    fun addCustomFood(name: String, category: String, safety: String) {
        if (name.isBlank()) return
        viewModelScope.launch { foodRepository.addCustomFood(name.trim(), category, safety) }
    }

    fun updateCustomFood(food: FoodEntity, name: String, category: String, safety: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            foodRepository.updateCustomFood(food.copy(name = name.trim(), category = category, safety = safety))
        }
    }

    fun deleteFood(id: String) {
        viewModelScope.launch { foodRepository.deleteCustomFood(id) }
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
    var editing by remember { mutableStateOf<FoodRowUi?>(null) }

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
                placeholder = { Text("搜索食物（含危险食物警示）…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
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
                    catOk && (q.isEmpty() || r.name.contains(q))
                }
                .sortedWith(
                    compareByDescending<FoodRowUi> { it.prefLevel == "LOVE" }
                        .thenByDescending { it.prefLevel == "LIKE" }
                        .thenBy { it.name },
                )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(visible, key = { it.id }) { r ->
                    FoodRow(r, onMark = { level -> vm.mark(r.id, level) }, onOpen = { editing = r })
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
        FoodFormSheet(
            title = "自定义食物",
            initial = null,
            readOnly = false,
            onDismiss = { showAdd = false },
            onSave = { name, catCode, safety ->
                vm.addCustomFood(name, catCode, safety)
                showAdd = false
            },
            onDelete = null,
        )
    }

    editing?.let { r ->
        val entity = FoodEntity(
            id = r.id,
            name = r.name,
            category = r.category,
            safety = r.safety,
            isBuiltin = r.isBuiltin,
            createdAt = 0L,
        )
        FoodFormSheet(
            title = if (r.isBuiltin) "食物详情" else "编辑自定义食物",
            initial = r,
            readOnly = r.isBuiltin,
            onDismiss = { editing = null },
            onSave = { name, catCode, safety ->
                vm.updateCustomFood(entity, name, catCode, safety)
                editing = null
            },
            onDelete = if (r.isBuiltin) null else {
                { vm.deleteFood(r.id); editing = null }
            },
        )
    }
}

/** 新增/编辑自定义食物的底单：名字 + 分类 + 安全等级；内置食物只读展示 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodFormSheet(
    title: String,
    initial: FoodRowUi?,
    readOnly: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, safety: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var cat by remember { mutableStateOf(initial?.category ?: "OTHER") }
    var safety by remember { mutableStateOf(initial?.safety ?: "SAFE") }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (readOnly) {
                Text(
                    "内置食物由应用维护（含安全警示知识库），仅可标记偏好",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                enabled = !readOnly,
                label = { Text("食物名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text("分类", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CATEGORY_OPTIONS.forEach { (code, label) ->
                    FilterChip(
                        selected = cat == code,
                        enabled = !readOnly,
                        onClick = { cat = code },
                        label = { Text(label) },
                    )
                }
            }
            Text("安全等级", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SAFETY_OPTIONS.forEach { (code, label) ->
                    FilterChip(
                        selected = safety == code,
                        enabled = !readOnly,
                        onClick = { safety = code },
                        label = { Text(label) },
                    )
                }
            }
            Button(
                onClick = {
                    onSave(name, cat, safety)
                },
                enabled = !readOnly && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("保存")
            }
            onDelete?.let { del ->
                TextButton(
                    onClick = del,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                ) {
                    Text("删除这个食物（喂食记录会保留，仅解除关联）", color = Color(0xFFA03030), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FoodRow(r: FoodRowUi, onMark: (String) -> Unit, onOpen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            SafetyBadge(r.safety)
            if (!r.isBuiltin) {
                Text("自定义", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 7.dp)) {
            PREF_OPTIONS.forEach { (code, label) ->
                val on = r.prefLevel == code
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .background(
                            if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small,
                        )
                        .clickable { onMark(code) }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                )
            }
        }
    }
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
