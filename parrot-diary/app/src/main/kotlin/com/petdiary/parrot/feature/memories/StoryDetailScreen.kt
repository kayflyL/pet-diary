package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
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

/** 一条日记的展示态 */
data class StoryEntryUi(
    val id: String,
    val petName: String,
    val accent: Color,
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
        val accentById = pets.associate { it.id to com.petdiary.parrot.core.designsystem.petAccentColor(it) }
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
                    accent = accentById[e.petId] ?: GreenInk,
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

/**
 * 故事详情 = 纸上的一页：巨型日期排版开头，照片横滑大图，日记为手账时间线
 * （时间在左轴、竖线相连、鸟名用各自强调色、铅笔编辑）。灵感：Day One 日视图 +
 * Google Photos 回忆页的大日期排版，融入 StoryTeller 纸面语言。
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
            val pagerState = rememberPagerState(pageCount = { day.photos.size })

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
                        buildString {
                            append("${day.photos.size} 张照片")
                            append(" · ")
                            append("${day.entries.size} 条日记")
                        },
                        fontSize = 12.sp,
                        color = SubInk,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))

                // ── 照片：横滑大图 + 页码指示 ──
                if (day.photos.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp),
                    ) { page ->
                        val p = day.photos[page]
                        AsyncImage(
                            model = java.io.File(p.path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(GreenInkSoft)
                                .clickable {
                                    viewer = ViewerState(day.photos, page)
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    ) {
                        day.photos.forEachIndexed { i, _ ->
                            val active = pagerState.currentPage == i
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                                    .background(
                                        if (active) GreenInk else SubInk.copy(alpha = 0.35f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── 手账时间线：日记 ──
                if (day.entries.isNotEmpty()) {
                    Text(
                        "手记",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenInk,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
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
                            .padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
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
                                    e.petName.ifBlank { "日记" },
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
                            if (e.text.isNotBlank()) {
                                Text(
                                    e.text,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    modifier = Modifier.padding(top = 8.dp),
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
                        "这一天只有照片，没有写下文字",
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

/** 日记编辑弹层：文字 + 心情标签（点击移除/新增） */
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
