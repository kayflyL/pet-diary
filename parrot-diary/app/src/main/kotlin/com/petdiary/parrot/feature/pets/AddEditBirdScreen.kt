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
import androidx.hilt.navigation.compose.hiltViewModel
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.core.model.BirdSources
import com.petdiary.parrot.core.model.GenderMethods
import com.petdiary.parrot.core.model.PersonalityTags
import com.petdiary.parrot.core.model.SpeciesCatalog
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBirdScreen(onDone: () -> Unit, vm: AddEditBirdViewModel = hiltViewModel()) {
    val form by vm.form.collectAsState()

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
            ChipSelector(
                label = "品种 *",
                options = SpeciesCatalog.all.map { it.name },
                selected = SpeciesCatalog.nameOf(form.speciesId).ifBlank { null },
                onSelect = { name ->
                    val sid = name?.let { n -> SpeciesCatalog.all.first { o -> o.name == n }.id }.orEmpty()
                    vm.update { f -> f.copy(speciesId = sid) }
                },
            )
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
            Spacer(Modifier.height(12.dp))
            ChipSelector(
                label = "性别判定方式（可选）",
                options = GenderMethods.presets,
                selected = form.genderMethod.orEmpty(),
                onSelect = { v -> vm.update { it.copy(genderMethod = v) } },
            )

            FormSection("个性")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonalityTags.presets.forEach { tag ->
                    FilterChip(
                        selected = tag in form.tags,
                        onClick = {
                            vm.update { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }
                        },
                        label = { Text(tag) },
                    )
                }
            }

            FormSection("强调色（这只鸟的专属颜色）")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BirdAccents.forEachIndexed { index, color ->
                    val selected = form.accentIndex == index
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { vm.update { it.copy(accentIndex = index) } },
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
