package com.petdiary.parrot.feature.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDatePickerState
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
import com.petdiary.parrot.core.designsystem.AccentPalette
import com.petdiary.parrot.core.model.BirdSources
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBirdScreen(onDone: () -> Unit, vm: AddEditBirdViewModel = hiltViewModel()) {
    val form by vm.form.collectAsState()
    val speciesList by vm.species.collectAsState()
    val presetTags by vm.presets.collectAsState()
    val context = LocalContext.current
    var showSpeciesDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEdit) "编辑档案" else "添加一只鸟") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Text("取消", style = MaterialTheme.typography.bodyMedium) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            FormSection("身份")
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text("名字 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text("品种 *", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedSpeciesName = speciesList.firstOrNull { it.id == form.speciesId }?.name
                speciesList.forEach { sp ->
                    FilterChip(
                        selected = sp.name == selectedSpeciesName,
                        onClick = {
                            vm.update {
                                it.copy(
                                    speciesId = sp.id,
                                    speciesName = sp.name,
                                    refWeightMinG = sp.weightMinG,
                                    refWeightMaxG = sp.weightMaxG,
                                )
                            }
                        },
                        label = { Text(sp.name) },
                    )
                }
                FilterChip(selected = false, onClick = { showSpeciesDialog = true }, label = { Text("＋ 自定义") })
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.morph,
                onValueChange = { v -> vm.update { it.copy(morph = v) } },
                label = { Text("花色（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.legBand,
                onValueChange = { v -> vm.update { it.copy(legBand = v) } },
                label = { Text("脚环号（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            ChipSelector(
                label = "来源（可选）",
                options = BirdSources.presets,
                selected = form.source,
                onSelect = { v -> vm.update { it.copy(source = v.orEmpty()) } },
            )

            FormSection("时间")
            DateField("出壳日 / 生日（可选）", form.birthEpochDay) { day ->
                vm.update { it.copy(birthEpochDay = day) }
            }
            Spacer(Modifier.height(12.dp))
            DateField("到家日（可选）", form.gotchaEpochDay) { day ->
                vm.update { it.copy(gotchaEpochDay = day) }
            }

            FormSection("生理")
            Text("性别", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("M" to "公", "F" to "母", "U" to "未知").forEach { (code, label) ->
                    FilterChip(
                        selected = form.gender == code,
                        onClick = { vm.update { it.copy(gender = code) } },
                        label = { Text(label) },
                    )
                }
            }


            FormSection("个性")
            Text("个性标签（＋ 可自定义，长按列表项可删）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (presetTags.map { it.tag } + form.tags).distinct().forEach { tag ->
                    FilterChip(
                        selected = tag in form.tags,
                        onClick = {
                            vm.update { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }
                        },
                        label = { Text(tag) },
                    )
                }
                FilterChip(selected = false, onClick = { showTagDialog = true }, label = { Text("＋ 自定义") })
            }

            FormSection("强调色（这只鸟的专属颜色）")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPalette.forEach { color ->
                    val argb = color.toArgb()
                    val selected = form.accentColor == argb ||
                        (form.accentColor == null && AccentPalette.getOrNull(form.accentIndex) == color)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { vm.update { it.copy(accentColor = argb) } },
                    )
                }
            }

            FormSection("备注")
            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> vm.update { it.copy(notes = v) } },
                label = { Text("会说话吗？有什么习惯？（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { vm.save(onDone) },
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (form.isEdit) "保存修改" else "创建档案", fontWeight = FontWeight.Bold)
            }
            if (!form.canSave && form.name.isBlank()) {
                Text(
                    "填好名字和品种即可保存，其余都可以以后再补",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSpeciesDialog) {
        var newName by remember { mutableStateOf("") }
        var minText by remember { mutableStateOf("") }
        var maxText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSpeciesDialog = false },
            title = { Text("自定义品种") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("品种名称") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("健康下限 g") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { maxText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("健康上限 g") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        "已有品种（点名称旁的 ✕ 删除；被鸟使用的不可删）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        speciesList.forEach { spItem ->
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(spItem.name) },
                                trailingIcon = {
                                    Text(
                                        "✕",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                vm.deleteSpecies(spItem.id) { ok ->
                                                    if (!ok) {
                                                        android.widget.Toast.makeText(context, "「${spItem.name}」正在被使用，不能删除", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(2.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val name = newName.trim()
                        val minG = minText.toDoubleOrNull()
                        val maxG = maxText.toDoubleOrNull()
                        vm.addSpecies(name, minG, maxG) { id ->
                            vm.update {
                                it.copy(speciesId = id, speciesName = name, refWeightMinG = minG, refWeightMaxG = maxG)
                            }
                            showSpeciesDialog = false
                        }
                    },
                ) { Text("添加并选用") }
            },
            dismissButton = { TextButton(onClick = { showSpeciesDialog = false }) { Text("取消") } },
        )
    }

    if (showTagDialog) {
        var newTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("自定义个性标签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("标签（如：会喊名字）") },
                        singleLine = true,
                    )
                    Text(
                        "预设池（点 ✕ 删除）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetTags.forEach { preset ->
                            FilterChip(
                                selected = preset.tag in form.tags,
                                onClick = {
                                    vm.update { it.copy(tags = if (preset.tag in it.tags) it.tags - preset.tag else it.tags + preset.tag) }
                                },
                                label = { Text(preset.tag) },
                                trailingIcon = {
                                    Text(
                                        "✕",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clickable { vm.deletePreset(preset.tag) }
                                            .padding(2.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newTag.isNotBlank(),
                    onClick = {
                        val tag = newTag.trim()
                        vm.addPreset(tag)
                        vm.update { it.copy(tags = it.tags + tag) }
                        newTag = ""
                        showTagDialog = false
                    },
                ) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun FormSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
    )
}

/** 单选 chips（替代原生下拉，与全 App 的 chips 语言统一）；再点已选中项取消选择 */
@Composable
private fun ChipSelector(label: String, options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(if (selected == option) null else option) },
                label = { Text(option) },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, epochDay: Long?, onPick: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val display = epochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()
    Box {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { show = true },
            trailingIcon = {
                IconButton(onClick = { show = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                }
            },
        )
        if (show) {
            val state = rememberDatePickerState(initialSelectedDateMillis = epochDay?.times(86_400_000L))
            DatePickerDialog(
                onDismissRequest = { show = false },
                confirmButton = {
                    TextButton(onClick = { show = false; state.selectedDateMillis?.let { onPick(it / 86_400_000L) } }) {
                        Text("确定")
                    }
                },
                dismissButton = { TextButton(onClick = { show = false }) { Text("取消") } },
            ) {
                DatePicker(state = state)
            }
        }
    }
}
