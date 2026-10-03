package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PlacePhotoUi(val id: String, val path: String, val date: LocalDate, val petName: String)

data class PlaceEntryUi(val entryId: String, val text: String, val date: LocalDate, val petName: String)

data class PlaceDetailUi(
    val name: String,
    val cover: String?,
    val photos: List<PlacePhotoUi>,
    val entries: List<PlaceEntryUi>,
    val visitDays: Int,
    val firstVisit: LocalDate?,
)

@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    diaryRepository: DiaryRepository,
    petRepository: PetRepository,
) : ViewModel() {
    private val key: String = savedStateHandle.get<String>("name").orEmpty()

    val ui: StateFlow<PlaceDetailUi> = combine(
        diaryRepository.observePlaceMedia(),
        petRepository.observeActive(),
    ) { rows, pets ->
        val nameById = pets.associate { it.id to it.name }
        val zone = ZoneId.systemDefault()
        // 命中规则：地点名相同，或同为此处未命名坐标聚合键（约百米粒度）
        val mine = rows.filter {
            it.placeName == key || (it.placeName == null && "%.3f,%.3f".format(it.lat, it.lng) == key)
        }
        val photos = mine
            .sortedBy { it.takenAt ?: it.happenedAt ?: 0L }
            .map { r ->
                PlacePhotoUi(
                    id = r.id,
                    path = r.path,
                    date = Instant.ofEpochMilli(r.takenAt ?: r.happenedAt ?: 0L).atZone(zone).toLocalDate(),
                    petName = nameById[r.petId].orEmpty(),
                )
            }
        val entries = mine
            .filter { it.entryId != null && !it.entryText.isNullOrBlank() }
            .distinctBy { it.entryId }
            .sortedByDescending { it.happenedAt ?: 0L }
            .map { r ->
                PlaceEntryUi(
                    entryId = r.entryId!!,
                    text = r.entryText!!,
                    date = Instant.ofEpochMilli(r.happenedAt ?: 0L).atZone(zone).toLocalDate(),
                    petName = nameById[r.petId].orEmpty(),
                )
            }
        PlaceDetailUi(
            name = mine.firstOrNull()?.placeName ?: key,
            cover = mine.lastOrNull()?.path,
            photos = photos,
            entries = entries,
            visitDays = photos.map { it.date }.distinct().size,
            firstVisit = photos.minByOrNull { it.date }?.date,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceDetailUi("", null, emptyList(), emptyList(), 0, null))
}

/** 地点详情页（原型 s-place）：头图压字 + 到访统计 + 我们的故事 + 照片墙 */
@Composable
fun PlaceDetailScreen(onBack: () -> Unit, vm: PlaceDetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var viewer by remember { mutableStateOf<ViewerState?>(null) }

    PaperTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(PaperBg)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "←",
                        fontSize = 20.sp,
                        color = PaperInk,
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(end = 14.dp),
                    )
                    Text("地点", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PaperInk)
                }

                // 头图：该地最新一张照片全出血 + 地点名压字（原型 .place-hero）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .aspectRatio(1.9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenInkSoft),
                ) {
                    if (ui.cover != null) {
                        AsyncImage(
                            model = java.io.File(ui.cover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x8C10291A))))
                            .padding(14.dp),
                    ) {
                        Text(
                            ui.name,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        val withWho = (ui.photos.map { it.petName } + ui.entries.map { it.petName })
                            .filter { it.isNotEmpty() }
                            .distinct()
                        if (withWho.isNotEmpty()) {
                            Text(
                                "和 ${withWho.joinToString("、")} 一起",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                }

                // 统计行：到访天数 / 照片 / 日记 / 首次到访
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCell("${ui.visitDays} 次", "到访")
                    StatCell("${ui.photos.size} 张", "照片")
                    StatCell("${ui.entries.size} 条", "日记")
                    StatCell(
                        ui.firstVisit?.let { "${it.monthValue} 月" } ?: "—",
                        "首次到访",
                    )
                }

                // 我们的故事（当地日记，纯排版）
                if (ui.entries.isNotEmpty()) {
                    Text(
                        "我们的故事",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaperInk,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp),
                    )
                    ui.entries.take(3).forEach { e ->
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            Text(
                                e.text,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                color = PaperInk,
                            )
                            Text(
                                buildString {
                                    if (e.petName.isNotEmpty()) append("${e.petName} · ")
                                    append("${e.date.year} 年 ${e.date.monthValue} 月 ${e.date.dayOfMonth} 日")
                                },
                                fontSize = 10.5.sp,
                                color = SubInk,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }

                // 这里的照片
                Text(
                    "这里的照片 · ${ui.photos.size} 张",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaperInk,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                )
                val rows = ui.photos.chunked(3)
                rows.forEach { rowPhotos ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowPhotos.forEach { p ->
                            AsyncImage(
                                model = java.io.File(p.path),
                                contentDescription = p.petName,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GreenInkSoft)
                                    .clickable {
                                        val start = ui.photos.indexOfFirst { it.id == p.id }.coerceAtLeast(0)
                                        viewer = ViewerState(
                                            ui.photos.map { PhotoUi(it.id, it.path, it.petName, it.date, null, null, ui.name) },
                                            start,
                                        )
                                    },
                                contentScale = ContentScale.Crop,
                            )
                        }
                        repeat(3 - rowPhotos.size) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        viewer?.let { state ->
            PhotoViewer(state = state, onDismiss = { viewer = null })
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PaperInk)
        Text(label, fontSize = 10.5.sp, color = SubInk, modifier = Modifier.padding(top = 2.dp))
    }
}
