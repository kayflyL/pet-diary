package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlaceRowUi(
    val key: String,
    val name: String,
    val photos: Int,
    val visitDays: Int,
    val lastDate: LocalDate?,
    val cover: String?,
)

data class PlacesGroupUi(val label: String, val rows: List<PlaceRowUi>)

@HiltViewModel
class PlacesViewModel @Inject constructor(
    diaryRepository: DiaryRepository,
) : ViewModel() {
    val groups: StateFlow<List<PlacesGroupUi>> = diaryRepository.observePlaceMedia().map { rows ->
        val zone = ZoneId.systemDefault()
        fun dayOf(millis: Long?): LocalDate = Instant.ofEpochMilli(millis ?: 0L).atZone(zone).toLocalDate()
        val named = rows.filter { !it.placeName.isNullOrBlank() }
        val byPlace = named.groupBy { it.placeName!! }
            .map { (name, list) ->
                val days = list.map { dayOf(it.takenAt ?: it.happenedAt) }.distinct()
                PlaceRowUi(
                    key = name,
                    name = name,
                    photos = list.size,
                    visitDays = days.size,
                    lastDate = days.maxOrNull(),
                    cover = list.lastOrNull()?.path,
                )
            }
            .sortedByDescending { it.photos }

        // 分组（原型 s-places）：大本营（照片最多）/ 常去（≥3 天）/ 今年去过 / 更早
        val out = mutableListOf<PlacesGroupUi>()
        val rest = byPlace.toMutableList()
        rest.firstOrNull()?.let { head ->
            out += PlacesGroupUi("🏠 大本营", listOf(head))
            rest.remove(head)
        }
        val frequent = rest.filter { it.visitDays >= 3 }
        if (frequent.isNotEmpty()) {
            out += PlacesGroupUi("常去", frequent)
            rest.removeAll(frequent.toSet())
        }
        val thisYear = LocalDate.now().year
        val recent = rest.filter { (it.lastDate?.year ?: 0) >= thisYear }
        if (recent.isNotEmpty()) {
            out += PlacesGroupUi("今年去过", recent)
            rest.removeAll(recent.toSet())
        }
        if (rest.isNotEmpty()) out += PlacesGroupUi("更早", rest)
        out
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** 全部地点检索页（原型 s-places）：搜索 / 分组 / 点进地点详情 */
@Composable
fun PlacesScreen(
    onBack: () -> Unit,
    onOpenPlace: (String) -> Unit,
    vm: PlacesViewModel = hiltViewModel(),
) {
    val groups by vm.groups.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "←",
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(10.dp),
            )
            Text("全部地点与故事", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("🔍 搜索地点名…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            groups.forEach { group ->
                val rows = group.rows.filter { it.name.contains(query.trim(), ignoreCase = true) }
                if (rows.isEmpty()) return@forEach
                item {
                    Text(
                        group.label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
                    )
                }
                items(rows.size, key = { rows[it].key }) { idx ->
                    val row = rows[idx]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlace(row.key) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (row.cover != null) {
                                AsyncImage(
                                    model = java.io.File(row.cover),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    row.name.take(1),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(row.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                buildString {
                                    append("${row.visitDays} 次到访")
                                    row.lastDate?.let { append(" · 最近 ${it.monthValue} 月 ${it.dayOfMonth} 日") }
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${row.photos} 张 ›", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
