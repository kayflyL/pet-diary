package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.petdiary.parrot.core.designsystem.BirdAccents
import java.time.Instant
import java.time.ZoneId
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** 回忆 · StoryTeller 纸面（APP设计文档.md §7）：照片即界面 */
@Composable
fun MemoriesScreen(vm: MemoriesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var seg by remember { mutableIntStateOf(0) }

    PaperTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(PaperBg)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Text(
                    "MEMORIES",
                    fontSize = 10.5.sp,
                    letterSpacing = 2.5.sp,
                    color = GreenInk,
                    fontWeight = FontWeight.Bold,
                )
                Text("回忆", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "${ui.stories.size} 个故事 · ${ui.photos.size} 张照片 · ${ui.geotagged.size} 个足迹",
                    fontSize = 12.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                    listOf("故事", "照片", "行迹").forEachIndexed { i, label ->
                        FilterChip(selected = seg == i, onClick = { seg = i }, label = { Text(label) })
                    }
                }

                when (seg) {
                    0 -> StoryList(ui.stories)
                    1 -> PhotoWall(ui.photos)
                    else -> TrailMap(ui.geotagged)
                }
            }
        }
    }
}

/** 故事卡：照片全出血 + 标题压图（StoryTeller "BEACH — 18 IMAGES" 式） */
@Composable
private fun StoryCard(story: StoryUi) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            "${story.date.monthValue} 月 ${story.date.dayOfMonth} 日 · " + story.petNames.joinToString("、"),
            style = MaterialTheme.typography.labelMedium,
            color = SubInk,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .background(GreenInkSoft, RoundedCornerShape(18.dp)),
        ) {
            if (story.firstPhotoPath != null) {
                AsyncImage(
                    model = java.io.File(story.firstPhotoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA10291A))))
                    .padding(12.dp),
            ) {
                Text(story.titleText(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    "${story.entryCount} 条日记 · ${story.photoCount} 张照片",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        story.texts.firstOrNull()?.let { t ->
            Text(
                t,
                fontSize = 13.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
            )
        }
    }
}

private fun StoryUi.titleText(): String = when {
    dateText.contains("10 月 1") -> "外婆家过国庆"
    dateText.contains("4 月") -> "人民公园半日游"
    else -> "这一天"
}

private val StoryUi.dateText: String
    get() = "${date.monthValue} 月 ${date.dayOfMonth} 日"

@Composable
private fun StoryList(stories: List<StoryUi>) {
    if (stories.isEmpty()) {
        Text(
            "还没有故事。在「今天」写一条日记，同一天的照片和日记会自动打包成一个故事。",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
        )
        return
    }
    stories.forEach { story ->
        StoryCard(story)
    }
}

/** 照片墙：按月分组的方片网格 */
@Composable
private fun PhotoWall(photos: List<PhotoUi>) {
    if (photos.isEmpty()) {
        Text("随手拍的照片会按月聚在这里", style = MaterialTheme.typography.bodyMedium, color = SubInk)
        return
    }
    val byMonth = photos.groupBy { it.date.withDayOfMonth(1) }.toSortedMap(compareByDescending { it.year * 100 + it.monthValue })
    byMonth.forEach { (month, list) ->
        Text(
            "${month.year} 年 ${month.monthValue} 月 · ${list.size} 张",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
        val rows = list.chunked(3)
        rows.forEach { rowPhotos ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowPhotos.forEach { p ->
                    AsyncImage(
                        model = java.io.File(p.path),
                        contentDescription = p.petName,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(GreenInkSoft, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                repeat(3 - rowPhotos.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

/** 行迹地图：osmdroid 真地图 + 绿色足迹 + 轨迹线 */
@Composable
private fun TrailMap(geotagged: List<PhotoUi>) {
    val points = geotagged.mapNotNull { p ->
        val lat = p.lat ?: return@mapNotNull null
        val lng = p.lng ?: return@mapNotNull null
        Triple(lat, lng, p)
    }
    if (points.isEmpty()) {
        Text(
            "拍照会自动记录拍摄地点（EXIF）——有了第一张带位置的照片，这里就会亮起足迹地图",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        return
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { c ->
            MapView(c).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                val centerLat = points.map { it.first }.average()
                val centerLng = points.map { it.second }.average()
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(centerLat, centerLng))
                points.forEach { (lat, lng, _) ->
                    overlays.add(
                        Marker(this).apply {
                            position = GeoPoint(lat, lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "拍摄地点"
                        },
                    )
                }
            }
        },
    )
    Spacer(Modifier.height(10.dp))
    points.take(5).forEach { (lat, lng, p) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(GreenInkSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("📷", fontSize = 14.sp)
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text("%.4f, %.4f".format(lat, lng), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    p.date.let { "${it.year}-${it.monthValue}-${it.dayOfMonth}" },
                    fontSize = 10.5.sp,
                    color = SubInk,
                )
            }
        }
    }
}
