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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.petdiary.parrot.data.DiaryRepository
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.feature.common.LucideIcon
import com.petdiary.parrot.feature.common.fluidEnter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 一条日记的展示态：自己的照片 + 自己的文案（朋友圈式，条目为原子） */
data class StoryEntryUi(
    val id: String,
    val petName: String,
    val accent: Color,
    val text: String,
    val tags: List<String>,
    val timeText: String,
    val photos: List<PhotoUi>,
)

data class StoryDayUi(
    val date: LocalDate,
    val photoCount: Int,
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
        val accentById = pets.associate { it.id to com.petdiary.parrot.core.designsystem.petAccentColor(it) }
        val dayEntries = entries
            .filter { it.happenedAt >= start && it.happenedAt < end }
            .sortedByDescending { it.happenedAt }
        val dayMedia = media
            .filter { m -> dayEntries.any { it.id == m.entryId } }
            .sortedByDescending { it.createdAt }
        StoryDayUi(
            date = date,
            photoCount = dayMedia.size,
            entries = dayEntries.map { e ->
                StoryEntryUi(
                    id = e.id,
                    petName = nameById[e.petId].orEmpty(),
                    accent = accentById[e.petId] ?: GreenInk,
                    text = e.text,
                    tags = e.tags
                        ?.trim('[', ']')
                        ?.split("\",\"")
                        ?.map { it.trim('"') }
                        ?.filter { it.isNotBlank() }
                        .orEmpty(),
                    timeText = Instant.ofEpochMilli(e.happenedAt).atZone(zone).toLocalTime().toString().take(5),
                    photos = dayMedia.filter { it.entryId == e.id }.map { m ->
                        PhotoUi(
                            id = m.id,
                            path = m.path,
                            petName = nameById[e.petId].orEmpty(),
                            date = date,
                            lat = m.lat,
                            lng = m.lng,
                            placeName = null,
                            takenAt = m.takenAt ?: m.createdAt,
                        )
                    },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 编辑日记文字与标签 */
    fun updateEntry(entryId: String, text: String, tags: List<String>) {
        viewModelScope.launch { writableDiaryRepository.updateEntryTextTags(entryId, text, tags) }
    }

    /** 删除单张照片（记录与文件一并移除） */
    fun deletePhoto(mediaId: String) {
        viewModelScope.launch { writableDiaryRepository.deleteMedia(mediaId) }
    }

    /** 删除整条日记（连带其照片） */
    fun deleteEntry(entryId: String) {
        viewModelScope.launch { writableDiaryRepository.deleteEntry(entryId) }
    }
}

/**
 * 故事详情 = 纸上的一页：巨型日期排版开头，朋友圈式的条目块
 * （每条故事 = 自己的照片 + 自己的文案，可删单张照片/整条），手账时间线呈现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(onBack: () -> Unit, vm: StoryDetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    var editing by remember { mutableStateOf<StoryEntryUi?>(null) }

    PaperTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
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
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── 巨型日期排版开头 ──
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${day.date.year} · " +
                            day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = SubInk,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${day.date.monthValue} 月 ${day.date.dayOfMonth} 日",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 46.sp,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "${day.photoCount} 张照片 · ${day.entries.size} 条故事",
                        fontSize = 12.sp,
                        color = SubInk,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))

                // ── 手账时间线：朋友圈式的条目块 ──
                day.entries.forEachIndexed { i, e ->
                    val lineColor = SubInk.copy(alpha = 0.3f)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .fluidEnter(i)
                            .drawBehind {
                                if (i < day.entries.size - 1) {
                                    val x = 53.dp.toPx()
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(x, 18.dp.toPx()),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1.2.dp.toPx(),
                                    )
                                }
                            }
                            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            e.timeText,
                            fontSize = 10.5.sp,
                            color = SubInk,
                            modifier = Modifier.width(48.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    e.petName.ifBlank { "故事" },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = e.accent,
                                    modifier = Modifier
                                        .background(e.accent.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp),
                                )
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(SubInk.copy(alpha = 0.08f), CircleShape)
                                        .clickable { editing = e },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LucideIcon("pen", SubInk, Modifier.size(14.dp))
                                }
                            }
                            // 照片：本条故事自己的
                            if (e.photos.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 10.dp),
                                ) {
                                    items(e.photos.size) { idx ->
                                        val p = e.photos[idx]
                                        Box {
                                            AsyncImage(
                                                model = java.io.File(p.path),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(104.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(GreenInkSoft)
                                                    .clickable { viewer = ViewerState(e.photos, idx) },
                                                contentScale = ContentScale.Crop,
                                            )
                                            Text(
                                                "✕",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .background(Color(0x99000000), CircleShape)
                                                    .clickable { vm.deletePhoto(p.id) }
                                                    .padding(3.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            if (e.text.isNotBlank()) {
                                Text(
                                    e.text,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            if (e.tags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 9.dp),
                                ) {
                                    e.tags.forEach { tag ->
                                        Text(
                                            "#$tag",
                                            fontSize = 11.sp,
                                            color = GreenInk,
                                            modifier = Modifier
                                                .background(GreenInkSoft, RoundedCornerShape(7.dp))
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
                        "这一天还没有故事",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubInk,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
                Spacer(Modifier.height(40.dp))
            }
        }

        viewer?.let { state ->
            PhotoViewer(state = state, onDismiss = { viewer = null })
        }

        editing?.let { entry ->
            StoryEditDialog(
                entry = entry,
                onDismiss = { editing = null },
                onSave = { text, tags ->
                    vm.updateEntry(entry.id, text, tags)
                    editing = null
                },
                onDelete = {
                    vm.deleteEntry(entry.id)
                    editing = null
                },
            )
        }
    }
}

/** 日记编辑弹层：文字 + 心情标签 + 删除整条（两步确认） */
@Composable
private fun StoryEditDialog(
    entry: StoryEntryUi,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(entry.text) }
    var tags by remember { mutableStateOf(entry.tags.toSet()) }
    var newTag by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑故事") },
        text = {
            Column {
                if (entry.photos.isNotEmpty()) {
                    Text(
                        "照片 ${entry.photos.size} 张（在详情页单张移除）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("这一刻的想法（可留空）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                TextButton(
                    onClick = {
                        if (confirmDelete) onDelete() else confirmDelete = true
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        if (confirmDelete) "再点一次确认删除（含 ${entry.photos.size} 张照片）" else "删除这条故事",
                        color = Color(0xFFC53939),
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() || tags.isNotEmpty() || entry.photos.isNotEmpty(),
                onClick = { onSave(text.trim(), tags.toList()) },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
