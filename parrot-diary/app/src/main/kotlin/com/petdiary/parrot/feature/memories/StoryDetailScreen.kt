package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.petdiary.parrot.data.DiaryRepository
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.local.DiaryEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 一条日记的展示态 */
data class StoryEntryUi(
    val id: String,
    val petName: String,
    val text: String,
    val tags: List<String>,
    val timeText: String,
)

data class StoryDayUi(
    val date: LocalDate,
    val photos: List<PhotoUi>,
    val entries: List<StoryEntryUi>,
)

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    diaryRepository: DiaryRepository,
    petRepository: PetRepository,
    private val writableDiaryRepository: DiaryRepository,
) : ViewModel() {
    private val epochDay: Long = savedStateHandle.get<Long>("epochDay") ?: 0L

    val ui: StateFlow<StoryDayUi?> = combine(
        diaryRepository.observeAllEntries(),
        diaryRepository.observeAllMedia(),
        petRepository.observeActive(),
    ) { entries, media, pets ->
        val date = LocalDate.ofEpochDay(epochDay)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val nameById = pets.associate { it.id to it.name }
        val dayEntries = entries
            .filter { it.happenedAt >= start && it.happenedAt < end }
            .sortedByDescending { it.happenedAt }
        val ids = dayEntries.map { it.id }.toSet()
        val dayMedia = media.filter { it.entryId in ids }.sortedByDescending { it.createdAt }
        StoryDayUi(
            date = date,
            photos = dayMedia.map { m ->
                val entry = dayEntries.firstOrNull { it.id == m.entryId }
                PhotoUi(
                    id = m.id,
                    path = m.path,
                    petName = nameById[entry?.petId].orEmpty(),
                    date = date,
                    lat = m.lat,
                    lng = m.lng,
                    placeName = null,
                    takenAt = m.takenAt ?: m.createdAt,
                )
            },
            entries = dayEntries.map { e ->
                StoryEntryUi(
                    id = e.id,
                    petName = nameById[e.petId].orEmpty(),
                    text = e.text,
                    tags = e.tags
                        ?.trim('[', ']')
                        ?.split("\",\"")
                        ?.map { it.trim('"') }
                        ?.filter { it.isNotBlank() }
                        .orEmpty(),
                    timeText = Instant.ofEpochMilli(e.happenedAt).atZone(zone).toLocalTime().toString().take(5),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 编辑日记文字与标签 */
    fun updateEntry(entryId: String, text: String, tags: List<String>) {
        viewModelScope.launch { writableDiaryRepository.updateEntryTextTags(entryId, text, tags) }
    }
}

/** 故事详情：同一天的照片 + 日记文案，可编辑文字与标签 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(onBack: () -> Unit, vm: StoryDetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    var editing by remember { mutableStateOf<StoryEntryUi?>(null) }
    val zone = ZoneId.systemDefault()

    PaperTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val d = ui?.date
                        Text(
                            if (d != null) "${d.year} 年 ${d.monthValue} 月 ${d.dayOfMonth} 日" else "故事",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("‹ 返回", style = MaterialTheme.typography.bodyMedium) }
                    },
                )
            },
        ) { padding ->
            val day = ui
            if (day == null) {
                Box(Modifier.fillMaxSize().padding(padding))
                return@Scaffold
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Text(
                    "${day.photos.size} 张照片 · ${day.entries.size} 条日记",
                    fontSize = 12.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                if (day.photos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(day.photos, key = { it.id }) { p ->
                            AsyncImage(
                                model = java.io.File(p.path),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GreenInkSoft)
                                    .clickable {
                                        val idx = day.photos.indexOfFirst { it.id == p.id }.coerceAtLeast(0)
                                        viewer = ViewerState(day.photos, idx)
                                    },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                day.entries.forEach { e ->
                    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (e.petName.isNotBlank()) e.petName else "日记",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(e.timeText, fontSize = 11.sp, color = SubInk)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "编辑",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { editing = e },
                                )
                            }
                            if (e.text.isNotBlank()) {
                                Text(
                                    e.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            if (e.tags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 10.dp),
                                ) {
                                    e.tags.forEach { tag ->
                                        Text(
                                            "#$tag",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (day.entries.isEmpty()) {
                    Text(
                        "这一天只有照片，没有日记文字",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubInk,
                    )
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        viewer?.let { state ->
            PhotoViewer(state = state, onDismiss = { viewer = null })
        }

        editing?.let { entry ->
            StoryEditDialog(
                initialText = entry.text,
                initialTags = entry.tags,
                onDismiss = { editing = null },
                onSave = { text, tags ->
                    vm.updateEntry(entry.id, text, tags)
                    editing = null
                },
            )
        }
    }
}

/** 日记编辑弹层：文字 + 心情标签（点击切换/新增） */
@Composable
private fun StoryEditDialog(
    initialText: String,
    initialTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var tags by remember { mutableStateOf(initialTags.toSet()) }
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑日记") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Text(
                    "心情标签（点击移除）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        FilterChip(selected = true, onClick = { tags = tags - tag }, label = { Text(tag) })
                    }
                    if (tags.isEmpty()) {
                        Text("暂无", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (tags.size < 6) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            label = { Text("新标签") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = newTag.isNotBlank(),
                            onClick = {
                                tags = tags + newTag.trim()
                                newTag = ""
                            },
                        ) { Text("添加") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() || tags.isNotEmpty(),
                onClick = { onSave(text.trim(), tags.toList()) },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
